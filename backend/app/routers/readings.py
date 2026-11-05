from datetime import datetime, timezone
from typing import List, Optional
from fastapi import APIRouter, Depends, HTTPException, Query, status
from sqlalchemy.orm import Session
from sqlalchemy import desc

from backend.app.database import get_db
from backend.app.models import Reading
from backend.app.schemas import ReadingCreate, ReadingResponse, DashboardSummary
from backend.app.services.ml_service import ml_service

router = APIRouter(prefix="/readings", tags=["Readings"])


@router.post("", response_model=ReadingResponse, status_code=status.HTTP_201_CREATED)
def create_reading(payload: ReadingCreate, db: Session = Depends(get_db)):
    """
    Submits a new pipe reading:
    - Scales features (Pressure, Refill).
    - Runs all 4 ML models to compute Leak probabilities.
    - Computes average probability and decides final verdict (Leak vs No Leak).
    - Stores the record in SQLite.
    """
    try:
        ml_result = ml_service.predict(payload.pressure, payload.refill)
    except Exception as e:
        raise HTTPException(
            status_code=status.HTTP_500_INTERNAL_SERVER_ERROR,
            detail=f"Machine learning inference failed: {str(e)}"
        )

    reading_ts = payload.timestamp or datetime.now(timezone.utc).isoformat()

    db_reading = Reading(
        pipe_id=payload.pipe_id.strip(),
        pressure=payload.pressure,
        refill=payload.refill,
        latitude=payload.latitude,
        longitude=payload.longitude,
        reading_timestamp=reading_ts,
        prob_logistic_regression=ml_result["probabilities"]["logistic_regression"],
        prob_decision_tree=ml_result["probabilities"]["decision_tree"],
        prob_random_forest=ml_result["probabilities"]["random_forest"],
        prob_svm=ml_result["probabilities"]["svm"],
        pred_logistic_regression=ml_result["predictions"]["logistic_regression"],
        pred_decision_tree=ml_result["predictions"]["decision_tree"],
        pred_random_forest=ml_result["predictions"]["random_forest"],
        pred_svm=ml_result["predictions"]["svm"],
        average_probability=ml_result["average_probability"],
        final_prediction=ml_result["final_prediction"],
        created_at=datetime.now(timezone.utc)
    )

    db.add(db_reading)
    db.commit()
    db.refresh(db_reading)
    return db_reading


@router.get("", response_model=List[ReadingResponse])
def get_readings(
    filter_status: Optional[str] = Query(None, alias="filter", description="Filter by 'Leak' or 'No Leak'"),
    search: Optional[str] = Query(None, description="Search by pipe_id"),
    limit: int = Query(100, ge=1, le=500),
    offset: int = Query(0, ge=0),
    db: Session = Depends(get_db)
):
    """Retrieves saved readings with optional filtering, search, and pagination."""
    query = db.query(Reading)

    if filter_status:
        query = query.filter(Reading.final_prediction.ilike(filter_status.strip()))

    if search:
        query = query.filter(Reading.pipe_id.ilike(f"%{search.strip()}%"))

    readings = query.order_by(desc(Reading.id)).offset(offset).limit(limit).all()
    return readings


@router.get("/{reading_id}", response_model=ReadingResponse)
def get_reading(reading_id: int, db: Session = Depends(get_db)):
    """Fetches details for a specific reading by ID."""
    reading = db.query(Reading).filter(Reading.id == reading_id).first()
    if not reading:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Reading with ID {reading_id} not found."
        )
    return reading


@router.delete("/{reading_id}", status_code=status.HTTP_200_OK)
def delete_reading(reading_id: int, db: Session = Depends(get_db)):
    """Deletes a reading by ID."""
    reading = db.query(Reading).filter(Reading.id == reading_id).first()
    if not reading:
        raise HTTPException(
            status_code=status.HTTP_404_NOT_FOUND,
            detail=f"Reading with ID {reading_id} not found."
        )

    db.delete(reading)
    db.commit()
    return {"message": "Reading deleted successfully", "id": reading_id}

