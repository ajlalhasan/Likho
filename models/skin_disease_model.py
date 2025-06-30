import tensorflow as tf
from tensorflow import keras
from tensorflow.keras import layers
import numpy as np
import os

def create_cnn_model(num_classes=8, input_shape=(224, 224, 3)):
    """
    Create a CNN model for skin disease classification
    This is a template model - in production, you would load a pre-trained model
    """
    model = keras.Sequential([
        # Input layer
        layers.Input(shape=input_shape),
        
        # Data augmentation layers
        layers.RandomFlip("horizontal"),
        layers.RandomRotation(0.1),
        layers.RandomZoom(0.1),
        
        # Normalization
        layers.Rescaling(1./255),
        
        # First convolutional block
        layers.Conv2D(32, 3, activation='relu'),
        layers.MaxPooling2D(),
        
        # Second convolutional block
        layers.Conv2D(64, 3, activation='relu'),
        layers.MaxPooling2D(),
        
        # Third convolutional block
        layers.Conv2D(128, 3, activation='relu'),
        layers.MaxPooling2D(),
        
        # Fourth convolutional block
        layers.Conv2D(256, 3, activation='relu'),
        layers.MaxPooling2D(),
        
        # Global average pooling
        layers.GlobalAveragePooling2D(),
        
        # Dropout for regularization
        layers.Dropout(0.5),
        
        # Dense layers
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.3),
        
        # Output layer
        layers.Dense(num_classes, activation='softmax')
    ])
    
    return model

def create_transfer_learning_model(num_classes=8, input_shape=(224, 224, 3)):
    """
    Create a transfer learning model using pre-trained weights
    """
    # Load pre-trained model without top layers
    base_model = keras.applications.EfficientNetB0(
        weights='imagenet',
        include_top=False,
        input_shape=input_shape
    )
    
    # Freeze base model layers
    base_model.trainable = False
    
    # Add custom top layers
    model = keras.Sequential([
        base_model,
        layers.GlobalAveragePooling2D(),
        layers.Dropout(0.3),
        layers.Dense(128, activation='relu'),
        layers.Dropout(0.2),
        layers.Dense(num_classes, activation='softmax')
    ])
    
    return model

def compile_model(model, learning_rate=0.001):
    """Compile the model with appropriate optimizer and loss function"""
    model.compile(
        optimizer=keras.optimizers.Adam(learning_rate=learning_rate),
        loss='categorical_crossentropy',
        metrics=['accuracy', 'top_3_accuracy']
    )
    return model

def create_data_generators(train_dir, val_dir, batch_size=32, image_size=(224, 224)):
    """
    Create data generators for training and validation
    This function would be used during model training
    """
    train_datagen = keras.preprocessing.image.ImageDataGenerator(
        rescale=1./255,
        rotation_range=20,
        width_shift_range=0.2,
        height_shift_range=0.2,
        shear_range=0.2,
        zoom_range=0.2,
        horizontal_flip=True,
        fill_mode='nearest'
    )
    
    val_datagen = keras.preprocessing.image.ImageDataGenerator(rescale=1./255)
    
    train_generator = train_datagen.flow_from_directory(
        train_dir,
        target_size=image_size,
        batch_size=batch_size,
        class_mode='categorical'
    )
    
    val_generator = val_datagen.flow_from_directory(
        val_dir,
        target_size=image_size,
        batch_size=batch_size,
        class_mode='categorical'
    )
    
    return train_generator, val_generator

def train_model(model, train_generator, val_generator, epochs=50):
    """
    Train the model with callbacks for better performance
    This function would be used during model training phase
    """
    callbacks = [
        keras.callbacks.ModelCheckpoint(
            'best_skin_disease_model.h5',
            monitor='val_accuracy',
            save_best_only=True,
            mode='max',
            verbose=1
        ),
        keras.callbacks.ReduceLROnPlateau(
            monitor='val_loss',
            factor=0.2,
            patience=5,
            min_lr=0.0001,
            verbose=1
        ),
        keras.callbacks.EarlyStopping(
            monitor='val_loss',
            patience=10,
            restore_best_weights=True,
            verbose=1
        )
    ]
    
    history = model.fit(
        train_generator,
        epochs=epochs,
        validation_data=val_generator,
        callbacks=callbacks,
        verbose=1
    )
    
    return history

def evaluate_model(model, test_generator):
    """Evaluate model performance on test data"""
    # Get predictions
    predictions = model.predict(test_generator)
    
    # Get true labels
    true_labels = test_generator.classes
    
    # Calculate metrics
    from sklearn.metrics import classification_report, confusion_matrix
    
    predicted_labels = np.argmax(predictions, axis=1)
    
    # Classification report
    class_names = list(test_generator.class_indices.keys())
    report = classification_report(true_labels, predicted_labels, target_names=class_names)
    
    # Confusion matrix
    cm = confusion_matrix(true_labels, predicted_labels)
    
    return report, cm, predictions

def save_model_info(model, filepath):
    """Save model architecture and training information"""
    model_info = {
        'architecture': model.to_json(),
        'total_params': model.count_params(),
        'trainable_params': sum([keras.backend.count_params(w) for w in model.trainable_weights]),
        'layers': len(model.layers),
        'input_shape': model.input_shape,
        'output_shape': model.output_shape
    }
    
    import json
    with open(filepath, 'w') as f:
        json.dump(model_info, f, indent=2)
    
    return model_info

# Training script template
def main_training_script():
    """
    Main training script - this would be run separately to train the model
    This is a template and would need actual dataset paths
    """
    print("Starting skin disease model training...")
    
    # Parameters
    NUM_CLASSES = 8
    INPUT_SHAPE = (224, 224, 3)
    BATCH_SIZE = 32
    EPOCHS = 50
    
    # Create model
    model = create_transfer_learning_model(NUM_CLASSES, INPUT_SHAPE)
    model = compile_model(model)
    
    # Print model summary
    model.summary()
    
    # Create data generators (paths would need to be specified)
    # train_generator, val_generator = create_data_generators(
    #     'data/train', 'data/val', BATCH_SIZE, INPUT_SHAPE[:2]
    # )
    
    # Train model
    # history = train_model(model, train_generator, val_generator, EPOCHS)
    
    # Save model
    # model.save('skin_disease_model.h5')
    # save_model_info(model, 'model_info.json')
    
    print("Model training completed!")

if __name__ == "__main__":
    main_training_script()
