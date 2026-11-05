import os
import pytest
from fastapi.testclient import TestClient

from backend.app.main import app
from backend.app.services.ml_service import ml_service
from backend.app.database import SessionLocal, Base, engine
from backend.app.models import Reading


@pytest.fixture(scope="session", autouse=True)
def setup_db():
    Base.metadata.create_all(bind=engine)
    yield


@pytest.fixture
def client():
    with TestClient(app) as c:
        yield c


def test_ml_service_artifacts_loaded():
    assert ml_service.is_loaded is True
    assert ml_service.scaler is not None
    assert len(ml_service.models) == 4
    for model_name in ["logistic_regression", "decision_tree", "random_forest", "svm"]:
        assert model_name in ml_service.models


def test_ml_service_prediction_normal():
    # Normal pressure is typically in the 45-65 range
    result = ml_service.predict(pressure=52.5, refill=0)
    assert "probabilities" in result
    assert "predictions" in result
    assert "average_probability" in result
    assert "final_prediction" in result

    probs = result["probabilities"]
    expected_avg = round((
        probs["logistic_regression"]
        + probs["decision_tree"]
        + probs["random_forest"]
        + probs["svm"]
    ) / 4.0, 4)

    assert result["average_probability"] == expected_avg
    assert result["final_prediction"] in ["Leak", "No Leak"]
    # Normal pressure should produce No Leak
    assert result["final_prediction"] == "No Leak"
    assert result["average_probability"] < 0.5


def test_ml_service_prediction_leak():
    # Very low pressure (e.g. 22.0) indicates a burst/leak
    result = ml_service.predict(pressure=22.0, refill=0)
    assert result["average_probability"] >= 0.5
    assert result["final_prediction"] == "Leak"


def test_health_endpoint(client):
    response = client.get("/api/v1/health")
    assert response.status_code == 200
    data = response.json()
    assert data["status"] == "healthy"
    assert data["models_loaded"] is True
    assert data["database_connected"] is True


def test_create_and_fetch_reading(client):
    payload = {
        "pipe_id": "TEST-PIPE-001",
        "pressure": 30.5,
        "refill": 0,
        "latitude": 12.9716,
        "longitude": 77.5946,
        "timestamp": "2026-11-05T22:30:00"
    }
    # Create reading
    post_res = client.post("/api/v1/readings", json=payload)
    assert post_res.status_code == 201
    created = post_res.json()
    assert created["pipe_id"] == "TEST-PIPE-001"
    assert "id" in created
    reading_id = created["id"]
    assert "average_probability" in created
    assert "final_prediction" in created
    assert created["final_prediction"] in ["Leak", "No Leak"]

    # Fetch reading by id
    get_res = client.get(f"/api/v1/readings/{reading_id}")
    assert get_res.status_code == 200
    fetched = get_res.json()
    assert fetched["id"] == reading_id
    assert fetched["pipe_id"] == "TEST-PIPE-001"

    # Fetch reading in list
    list_res = client.get("/api/v1/readings")
    assert list_res.status_code == 200
    items = list_res.json()
    assert any(item["id"] == reading_id for item in items)


def test_dashboard_summary(client):
    res = client.get("/api/v1/dashboard/summary")
    assert res.status_code == 200
    data = res.json()
    assert "total_readings" in data
    assert "leaks_detected" in data
    assert "normal_readings" in data
    assert "leak_rate_percentage" in data
    assert "recent_readings" in data
    assert data["total_readings"] >= 1


def test_delete_reading(client):
    # Create a reading to delete
    payload = {
        "pipe_id": "DELETE-ME",
        "pressure": 55.0,
        "refill": 0,
        "latitude": 12.0,
        "longitude": 77.0
    }
    create_res = client.post("/api/v1/readings", json=payload)
    item_id = create_res.json()["id"]

    # Delete it
    del_res = client.delete(f"/api/v1/readings/{item_id}")
    assert del_res.status_code == 200

    # Ensure 404 when fetching again
    get_res = client.get(f"/api/v1/readings/{item_id}")
    assert get_res.status_code == 404


def test_database_persistence():
    # Insert via a session, close session, re-query from a fresh session
    db1 = SessionLocal()
    ml_res = ml_service.predict(50.0, 0)
    reading = Reading(
        pipe_id="PERSISTENCE-CHECK",
        pressure=50.0,
        refill=0,
        latitude=10.0,
        longitude=20.0,
        reading_timestamp="2026-11-05T00:00:00",
        prob_logistic_regression=ml_res["probabilities"]["logistic_regression"],
        prob_decision_tree=ml_res["probabilities"]["decision_tree"],
        prob_random_forest=ml_res["probabilities"]["random_forest"],
        prob_svm=ml_res["probabilities"]["svm"],
        pred_logistic_regression=ml_res["predictions"]["logistic_regression"],
        pred_decision_tree=ml_res["predictions"]["decision_tree"],
        pred_random_forest=ml_res["predictions"]["random_forest"],
        pred_svm=ml_res["predictions"]["svm"],
        average_probability=ml_res["average_probability"],
        final_prediction=ml_res["final_prediction"]
    )
    db1.add(reading)
    db1.commit()
    r_id = reading.id
    db1.close()

    # Open completely new session
    db2 = SessionLocal()
    persisted = db2.query(Reading).filter(Reading.id == r_id).first()
    assert persisted is not None
    assert persisted.pipe_id == "PERSISTENCE-CHECK"
    # Clean up test row
    db2.delete(persisted)
    db2.commit()
    db2.close()

