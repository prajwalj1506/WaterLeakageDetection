from datetime import datetime, timezone
from contextlib import asynccontextmanager
from fastapi import FastAPI
from fastapi.middleware.cors import CORSMiddleware

from backend.app.config import PROJECT_NAME, VERSION, API_V1_PREFIX
from backend.app.database import engine, Base
from backend.app.schemas import HealthResponse
from backend.app.services.ml_service import ml_service
from backend.app.routers import readings, dashboard


@asynccontextmanager
async def lifespan(app: FastAPI):
    # Ensure database tables exist
    Base.metadata.create_all(bind=engine)
    # Ensure ML models are loaded
    if not ml_service.is_loaded:
        ml_service.load_artifacts()
    yield


app = FastAPI(
    title=PROJECT_NAME,
    version=VERSION,
    description="Backend API for Water Leakage Detection using 4-Model ML Probability Averaging Ensemble",
    lifespan=lifespan
)

# Enable CORS for local Android development (emulator and LAN devices)
app.add_middleware(
    CORSMiddleware,
    allow_origins=["*"],
    allow_credentials=True,
    allow_methods=["*"],
    allow_headers=["*"],
)

# Include Routers under /api/v1
app.include_router(readings.router, prefix=API_V1_PREFIX)
app.include_router(dashboard.router, prefix=API_V1_PREFIX)


@app.get(f"{API_V1_PREFIX}/health", response_model=HealthResponse, tags=["Health"])
def health_check():
    """Health check endpoint reporting API status, loaded models, and DB status."""
    return HealthResponse(
        status="healthy",
        version=VERSION,
        models_loaded=ml_service.is_loaded,
        database_connected=True,
        timestamp=datetime.now(timezone.utc).isoformat()
    )


@app.get("/", tags=["Root"])
def root():
    return {
        "service": PROJECT_NAME,
        "version": VERSION,
        "docs_url": "/docs",
        "health_url": f"{API_V1_PREFIX}/health"
    }

