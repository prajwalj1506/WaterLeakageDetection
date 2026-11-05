import os
from pathlib import Path
from typing import Dict, Any, Tuple
import joblib
import numpy as np
import pandas as pd
from backend.app.config import ARTIFACTS_DIR


class MLService:
    def __init__(self, artifacts_dir: str = ARTIFACTS_DIR):
        self.artifacts_dir = Path(artifacts_dir)
        self.scaler = None
        self.models = {}
        self._is_loaded = False
        self.load_artifacts()

    def load_artifacts(self):
        """Loads the fitted scaler and the four trained ML models from artifacts dir."""
        scaler_path = self.artifacts_dir / "scaler.joblib"
        lr_path = self.artifacts_dir / "logistic_regression.joblib"
        dt_path = self.artifacts_dir / "decision_tree.joblib"
        rf_path = self.artifacts_dir / "random_forest.joblib"
        svm_path = self.artifacts_dir / "svm.joblib"

        required_files = [scaler_path, lr_path, dt_path, rf_path, svm_path]
        for f in required_files:
            if not f.exists():
                raise FileNotFoundError(f"Required ML artifact missing: {f}")

        self.scaler = joblib.load(scaler_path)
        self.models = {
            "logistic_regression": joblib.load(lr_path),
            "decision_tree": joblib.load(dt_path),
            "random_forest": joblib.load(rf_path),
            "svm": joblib.load(svm_path),
        }
        self._is_loaded = True

    @property
    def is_loaded(self) -> bool:
        return self._is_loaded and self.scaler is not None and len(self.models) == 4

    def predict(self, pressure: float, refill: int) -> Dict[str, Any]:
        """
        Executes prediction on the 2 features:
        1. Scales input features with StandardScaler.
        2. Computes predict_proba on all four models for the Leak class (1).
        3. Computes the unweighted average probability: (LR + DT + RF + SVM) / 4.
        4. Applies the 0.5 decision threshold.
        """
        if not self.is_loaded:
            raise RuntimeError("ML models are not loaded.")

        # Prepare DataFrame with matching feature names from training
        X_df = pd.DataFrame(
            [[float(pressure), float(refill)]],
            columns=["Pressure", "Refill"]
        )

        # Standardize features using the pre-fitted scaler
        X_scaled = self.scaler.transform(X_df)

        probabilities = {}
        predictions = {}

        for key, model in self.models.items():
            proba = model.predict_proba(X_scaled)[0]

            # Dynamically identify the column corresponding to class 1 ('Leak')
            if hasattr(model, "classes_"):
                classes = list(model.classes_)
                if 1 in classes:
                    leak_idx = classes.index(1)
                else:
                    leak_idx = 1
            else:
                leak_idx = 1

            leak_prob = float(proba[leak_idx])
            probabilities[key] = round(leak_prob, 4)

            # Model prediction: Leak if prob >= 0.5, else No Leak
            model_pred_raw = model.predict(X_scaled)[0]
            predictions[key] = "Leak" if int(model_pred_raw) == 1 else "No Leak"

        # Unweighted average of the four leak probabilities
        avg_probability = float(
            (
                probabilities["logistic_regression"]
                + probabilities["decision_tree"]
                + probabilities["random_forest"]
                + probabilities["svm"]
            )
            / 4.0
        )
        avg_probability = round(avg_probability, 4)

        # Ensemble decision rule: >= 0.5 indicates Leak
        final_prediction = "Leak" if avg_probability >= 0.5 else "No Leak"

        return {
            "probabilities": probabilities,
            "predictions": predictions,
            "average_probability": avg_probability,
            "final_prediction": final_prediction,
        }


# Global singleton instance
ml_service = MLService()
