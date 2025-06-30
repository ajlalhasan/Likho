import tensorflow as tf
import numpy as np
from PIL import Image
import streamlit as st
import os

# Disease classes that the model can predict
DISEASE_CLASSES = [
    "Acne",
    "Eczema", 
    "Melanoma",
    "Psoriasis",
    "Rosacea",
    "Seborrheic Dermatitis",
    "Basal Cell Carcinoma",
    "Normal Skin"
]

# Disease information and recommendations
DISEASE_INFO = {
    "Acne": {
        "description": "A skin condition that occurs when hair follicles are clogged with oil and dead skin cells.",
        "severity": "Mild to Moderate",
        "specialist": "Dermatology"
    },
    "Eczema": {
        "description": "A condition that makes skin red and itchy, commonly in children but can occur at any age.",
        "severity": "Mild to Severe",
        "specialist": "Dermatology"
    },
    "Melanoma": {
        "description": "The most serious type of skin cancer that develops in melanocytes.",
        "severity": "Severe - Requires Immediate Attention",
        "specialist": "Dermatology"
    },
    "Psoriasis": {
        "description": "An autoimmune condition that causes rapid skin cell production resulting in scaling.",
        "severity": "Moderate to Severe",
        "specialist": "Dermatology"
    },
    "Rosacea": {
        "description": "A common skin condition that causes redness and visible blood vessels in the face.",
        "severity": "Mild to Moderate",
        "specialist": "Dermatology"
    },
    "Seborrheic Dermatitis": {
        "description": "A common skin condition that mainly affects the scalp, causing scaly, itchy rash.",
        "severity": "Mild to Moderate",
        "specialist": "Dermatology"
    },
    "Basal Cell Carcinoma": {
        "description": "A type of skin cancer that begins in the basal cells.",
        "severity": "Moderate - Requires Medical Attention",
        "specialist": "Dermatology"
    },
    "Normal Skin": {
        "description": "Healthy skin with no visible signs of disease or condition.",
        "severity": "None",
        "specialist": "None"
    }
}

def load_model():
    """Load the pre-trained skin disease detection model"""
    try:
        # In a real implementation, you would load your trained model here
        # For now, we'll create a mock model structure
        model_path = os.getenv("MODEL_PATH", "models/skin_disease_model.h5")
        
        if os.path.exists(model_path):
            model = tf.keras.models.load_model(model_path)
            return model
        else:
            # Create a simple mock model for demonstration
            # In production, replace this with your actual trained model
            st.warning("⚠️ Model file not found. Using simulation mode for demonstration.")
            return None
    except Exception as e:
        st.error(f"Error loading model: {str(e)}")
        return None

def preprocess_image(image):
    """Preprocess uploaded image for model prediction"""
    try:
        # Convert to RGB if necessary
        if image.mode != 'RGB':
            image = image.convert('RGB')
        
        # Resize to model input size (assuming 224x224)
        image = image.resize((224, 224))
        
        # Convert to numpy array
        img_array = np.array(image)
        
        # Normalize pixel values to [0, 1]
        img_array = img_array.astype(np.float32) / 255.0
        
        # Add batch dimension
        img_array = np.expand_dims(img_array, axis=0)
        
        return img_array
    except Exception as e:
        st.error(f"Error preprocessing image: {str(e)}")
        return None

def predict_disease(model, processed_image):
    """Predict skin disease from processed image"""
    try:
        if model is None:
            # Mock prediction for demonstration
            # In production, this would be replaced with actual model prediction
            predictions = np.random.rand(len(DISEASE_CLASSES))
            predictions = predictions / np.sum(predictions)  # Normalize to probabilities
        else:
            # Real model prediction
            predictions = model.predict(processed_image)[0]
        
        # Get top prediction
        predicted_class_idx = np.argmax(predictions)
        predicted_disease = DISEASE_CLASSES[predicted_class_idx]
        confidence = predictions[predicted_class_idx]
        
        # Get all predictions with confidence scores
        all_predictions = []
        for i, disease in enumerate(DISEASE_CLASSES):
            all_predictions.append({
                'disease': disease,
                'confidence': float(predictions[i])
            })
        
        # Sort by confidence
        all_predictions.sort(key=lambda x: x['confidence'], reverse=True)
        
        return {
            'predicted_disease': predicted_disease,
            'confidence': float(confidence),
            'all_predictions': all_predictions
        }
    
    except Exception as e:
        st.error(f"Error during prediction: {str(e)}")
        return None

def get_disease_info(disease_name):
    """Get information about a specific disease"""
    return DISEASE_INFO.get(disease_name, {
        "description": "Information not available for this condition.",
        "severity": "Unknown",
        "specialist": "General Medicine"
    })

def get_treatment_recommendations(disease_name):
    """Get treatment recommendations for a disease"""
    recommendations = {
        "Acne": [
            "Use gentle, non-comedogenic skincare products",
            "Avoid picking or squeezing pimples",
            "Consider topical treatments with salicylic acid or benzoyl peroxide",
            "Consult a dermatologist for persistent cases"
        ],
        "Eczema": [
            "Keep skin moisturized with fragrance-free lotions",
            "Avoid known triggers (certain fabrics, soaps, stress)",
            "Use mild, unscented soaps and detergents",
            "Consider antihistamines for itching"
        ],
        "Melanoma": [
            "⚠️ URGENT: Consult a dermatologist immediately",
            "Avoid sun exposure and use high SPF sunscreen",
            "Do not delay medical consultation",
            "Early detection and treatment are crucial"
        ],
        "Psoriasis": [
            "Keep skin moisturized",
            "Avoid triggers like stress, infections, and certain medications",
            "Consider phototherapy under medical supervision",
            "Consult a dermatologist for appropriate treatment options"
        ],
        "Rosacea": [
            "Identify and avoid personal triggers",
            "Use gentle skincare products",
            "Wear broad-spectrum sunscreen daily",
            "Avoid harsh scrubs and alcohol-based products"
        ],
        "Seborrheic Dermatitis": [
            "Use medicated shampoos with selenium sulfide or ketoconazole",
            "Keep affected areas clean and dry",
            "Avoid harsh soaps and hot water",
            "Consider antifungal treatments"
        ],
        "Basal Cell Carcinoma": [
            "Consult a dermatologist promptly for proper evaluation",
            "Protect skin from UV radiation",
            "Regular skin self-examinations",
            "Follow up with medical professional for treatment options"
        ],
        "Normal Skin": [
            "Maintain good skincare routine",
            "Use sunscreen daily",
            "Stay hydrated and eat a balanced diet",
            "Regular skin checks for any changes"
        ]
    }
    
    return recommendations.get(disease_name, [
        "Consult a healthcare professional for proper diagnosis",
        "Maintain good general skincare practices",
        "Monitor symptoms and seek medical advice if condition worsens"
    ])
