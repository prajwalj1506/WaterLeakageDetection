import os
from pathlib import Path

# Resolve base directories safely
BASE_DIR = Path(__file__).resolve().parent.parent
ARTIFACTS_DIR = os.getenv("ARTIFACTS_DIR", str(BASE_DIR / "artifacts"))
DATABASE_PATH = os.getenv("DATABASE_PATH", str(BASE_DIR / "water_leakage.db"))
DATABASE_URL = f"sqlite:///{DATABASE_PATH}"

API_V1_PREFIX = "/api/v1"
PROJECT_NAME = "Water Leakage Detection API"
VERSION = "1.0.0"

