from datetime import datetime
from typing import List, Optional
from pydantic import BaseModel, Field, ConfigDict, field_validator


class ReadingCreate(BaseModel):
    pipe_id: str = Field(..., min_length=1, max_length=64, description="Pipe or Tank Identifier", examples=["PIPE-101"])
    pressure: float = Field(..., ge=0.0, le=200.0, description="Pressure reading value", examples=[52.5])
    refill: int = Field(..., ge=0, le=1, description="Refill status: 0 (No) or 1 (Yes)", examples=[0])
    latitude: float = Field(..., ge=-90.0, le=90.0, description="Latitude coordinate", examples=[12.9716])
    longitude: float = Field(..., ge=-180.0, le=180.0, description="Longitude coordinate", examples=[77.5946])
    timestamp: Optional[str] = Field(None, description="ISO or custom timestamp string. Defaults to current UTC if omitted.")

    @field_validator("refill")
    @classmethod
    def validate_refill(cls, v: int) -> int:
        if v not in (0, 1):
            raise ValueError("refill must be 0 or 1")
        return v


class ModelProbabilityDetail(BaseModel):
    model_name: str
    leak_probability: float
    prediction: str


class ReadingResponse(BaseModel):
    id: int
    pipe_id: str
    pressure: float
    refill: int
    latitude: float
    longitude: float
    reading_timestamp: str

    prob_logistic_regression: float
    prob_decision_tree: float
    prob_random_forest: float
    prob_svm: float

    pred_logistic_regression: str
    pred_decision_tree: str
    pred_random_forest: str
    pred_svm: str

    average_probability: float
    final_prediction: str
    created_at: datetime

    model_config = ConfigDict(from_attributes=True)


class DashboardSummary(BaseModel):
    total_readings: int
    leaks_detected: int
    normal_readings: int
    leak_rate_percentage: float
    recent_readings: List[ReadingResponse]


class HealthResponse(BaseModel):
    status: str
    version: str
    models_loaded: bool
    database_connected: bool
    timestamp: str
