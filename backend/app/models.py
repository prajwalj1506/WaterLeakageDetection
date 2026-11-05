from datetime import datetime
from sqlalchemy import Column, Integer, Float, String, DateTime
from backend.app.database import Base


class Reading(Base):
    __tablename__ = "readings"

    id = Column(Integer, primary_key=True, index=True, autoincrement=True)
    pipe_id = Column(String(64), index=True, nullable=False)
    pressure = Column(Float, nullable=False)
    refill = Column(Integer, nullable=False)  # 0 or 1
    latitude = Column(Float, nullable=False)
    longitude = Column(Float, nullable=False)
    reading_timestamp = Column(String(64), nullable=False)

    # Individual model probabilities (leak probability: 0.0 to 1.0)
    prob_logistic_regression = Column(Float, nullable=False)
    prob_decision_tree = Column(Float, nullable=False)
    prob_random_forest = Column(Float, nullable=False)
    prob_svm = Column(Float, nullable=False)

    # Individual model predictions ("Leak" or "No Leak")
    pred_logistic_regression = Column(String(16), nullable=False)
    pred_decision_tree = Column(String(16), nullable=False)
    pred_random_forest = Column(String(16), nullable=False)
    pred_svm = Column(String(16), nullable=False)

    # Ensemble results
    average_probability = Column(Float, nullable=False)
    final_prediction = Column(String(16), index=True, nullable=False)  # "Leak" or "No Leak"

    # Audit timestamp
    created_at = Column(DateTime, default=datetime.utcnow, nullable=False)

