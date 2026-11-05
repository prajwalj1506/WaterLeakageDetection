from fastapi import APIRouter, Depends
from sqlalchemy.orm import Session
from sqlalchemy import desc

from backend.app.database import get_db
from backend.app.models import Reading
from backend.app.schemas import DashboardSummary, ReadingResponse

router = APIRouter(prefix="/dashboard", tags=["Dashboard"])


@router.get("/summary", response_model=DashboardSummary)
def get_dashboard_summary(db: Session = Depends(get_db)):
    """Computes aggregate summary metrics and recent readings for the app dashboard."""
    total = db.query(Reading).count()
    leaks = db.query(Reading).filter(Reading.final_prediction == "Leak").count()
    normal = db.query(Reading).filter(Reading.final_prediction == "No Leak").count()

    leak_rate = round((leaks / total * 100.0), 2) if total > 0 else 0.0

    recent_readings = (
        db.query(Reading)
        .order_by(desc(Reading.id))
        .limit(10)
        .all()
    )

    return DashboardSummary(
        total_readings=total,
        leaks_detected=leaks,
        normal_readings=normal,
        leak_rate_percentage=leak_rate,
        recent_readings=recent_readings
    )

