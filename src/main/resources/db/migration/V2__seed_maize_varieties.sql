-- Insert default maize varieties for Zimbabwe
INSERT INTO maize_varieties (name, maturity_days, optimal_temperature_min, optimal_temperature_max, drought_resistance, disease_resistance, description, created_at, updated_at) VALUES
                                                                                                                                                                                     ('ZM 523', 120, 18.0, 30.0, true, 'Gray Leaf Spot, Northern Corn Leaf Blight', 'High-yielding drought-tolerant variety suitable for medium to low rainfall areas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('ZM 625', 125, 20.0, 32.0, false, 'Maize Streak Virus, Common Rust', 'High-yielding variety for high potential areas with good rainfall', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('ZM 309', 90, 18.0, 28.0, true, 'Gray Leaf Spot', 'Early maturing drought-tolerant variety for short season areas', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('ZM 421', 105, 19.0, 29.0, false, 'Northern Corn Leaf Blight, Common Rust', 'Medium-season variety with good adaptability to various conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('ZM 701', 140, 20.0, 33.0, false, 'Maize Streak Virus, Gray Leaf Spot', 'Late maturing high-yielding variety for irrigated conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('PAN 4M-19', 115, 18.0, 30.0, true, 'Gray Leaf Spot, Turcicum Leaf Blight', 'Commercial hybrid with excellent drought tolerance and disease resistance', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('SC Duma 43', 130, 19.0, 31.0, false, 'Northern Corn Leaf Blight, Common Rust', 'High-yielding hybrid suitable for commercial production', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('ZM 607', 100, 17.0, 28.0, true, 'Gray Leaf Spot, Maize Streak Virus', 'Early to medium maturing drought-tolerant variety', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('Pioneer 30G19', 118, 20.0, 32.0, true, 'Gray Leaf Spot, Northern Corn Leaf Blight, Common Rust', 'Premium hybrid with excellent stress tolerance and yield potential', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
                                                                                                                                                                                     ('Dekalb DKC 80-40', 135, 21.0, 34.0, false, 'Northern Corn Leaf Blight, Southern Rust', 'Late season hybrid for high-yield potential under optimal conditions', CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);

-- Create indexes for better performance on frequently queried fields
CREATE INDEX IF NOT EXISTS idx_maize_varieties_drought_resistance ON maize_varieties(drought_resistance);
CREATE INDEX IF NOT EXISTS idx_maize_varieties_maturity_days ON maize_varieties(maturity_days);
CREATE INDEX IF NOT EXISTS idx_maize_varieties_name ON maize_varieties(name);

-- Add additional indexes for yield history queries
CREATE INDEX IF NOT EXISTS idx_yield_history_harvest_date ON yield_history(harvest_date);
CREATE INDEX IF NOT EXISTS idx_yield_history_yield_tons ON yield_history(yield_tons_per_hectare);

-- Add indexes for weather data queries
CREATE INDEX IF NOT EXISTS idx_weather_data_date ON weather_data(date);
CREATE INDEX IF NOT EXISTS idx_weather_data_source ON weather_data(source);

-- Add indexes for recommendations
CREATE INDEX IF NOT EXISTS idx_recommendations_category ON recommendations(category);
CREATE INDEX IF NOT EXISTS idx_recommendations_priority ON recommendations(priority);
CREATE INDEX IF NOT EXISTS idx_recommendations_date ON recommendations(recommendation_date);
CREATE INDEX IF NOT EXISTS idx_recommendations_viewed ON recommendations(is_viewed);
CREATE INDEX IF NOT EXISTS idx_recommendations_implemented ON recommendations(is_implemented);

-- Add indexes for soil data
CREATE INDEX IF NOT EXISTS idx_soil_data_sample_date ON soil_data(sample_date);
CREATE INDEX IF NOT EXISTS idx_soil_data_soil_type ON soil_data(soil_type);

-- Add indexes for predictions
CREATE INDEX IF NOT EXISTS idx_yield_predictions_date ON yield_predictions(prediction_date);
CREATE INDEX IF NOT EXISTS idx_yield_predictions_confidence ON yield_predictions(confidence_percentage);

-- Add indexes for planting sessions
CREATE INDEX IF NOT EXISTS idx_planting_sessions_planting_date ON planting_sessions(planting_date);
CREATE INDEX IF NOT EXISTS idx_planting_sessions_harvest_date ON planting_sessions(expected_harvest_date);
CREATE INDEX IF NOT EXISTS idx_planting_sessions_variety ON planting_sessions(maize_variety_id);