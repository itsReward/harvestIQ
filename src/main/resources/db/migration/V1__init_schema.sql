-- Create Users table
CREATE TABLE users (
                       id BIGSERIAL PRIMARY KEY,
                       username VARCHAR(50) NOT NULL UNIQUE,
                       email VARCHAR(100) NOT NULL UNIQUE,
                       password_hash VARCHAR(255) NOT NULL,
                       first_name VARCHAR(50),
                       last_name VARCHAR(50),
                       phone_number VARCHAR(20),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       last_login TIMESTAMP
);

-- Create Farms table
CREATE TABLE farms (
                       id BIGSERIAL PRIMARY KEY,
                       user_id BIGINT NOT NULL REFERENCES users(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                       name VARCHAR(100) NOT NULL,
                       location VARCHAR(100) NOT NULL,
                       size_hectares NUMERIC(10, 2) NOT NULL,
                       latitude NUMERIC(10, 6),
                       longitude NUMERIC(10, 6),
                       elevation NUMERIC(10, 2),
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Soil Data table
CREATE TABLE soil_data (
                           id BIGSERIAL PRIMARY KEY,
                           farm_id BIGINT NOT NULL REFERENCES farms(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                           soil_type VARCHAR(50) NOT NULL,
                           ph_level NUMERIC(4, 2),
                           organic_matter_percentage NUMERIC(5, 2),
                           nitrogen_content NUMERIC(5, 2),
                           phosphorus_content NUMERIC(5, 2),
                           potassium_content NUMERIC(5, 2),
                           moisture_content NUMERIC(5, 2),
                           sample_date DATE NOT NULL,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Maize Varieties table
CREATE TABLE maize_varieties (
                                 id BIGSERIAL PRIMARY KEY,
                                 name VARCHAR(100) NOT NULL,
                                 maturity_days INTEGER NOT NULL,
                                 optimal_temperature_min NUMERIC(4, 1),
                                 optimal_temperature_max NUMERIC(4, 1),
                                 drought_resistance BOOLEAN NOT NULL,
                                 disease_resistance VARCHAR(100),
                                 description TEXT,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Planting Sessions table
CREATE TABLE planting_sessions (
                                   id BIGSERIAL PRIMARY KEY,
                                   farm_id BIGINT NOT NULL REFERENCES farms(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                                   maize_variety_id BIGINT NOT NULL REFERENCES maize_varieties(id),  -- Changed from INTEGER to BIGINT
                                   planting_date DATE NOT NULL,
                                   expected_harvest_date DATE,
                                   seed_rate_kg_per_hectare NUMERIC(6, 2),
                                   row_spacing_cm INTEGER,
                                   fertilizer_type VARCHAR(100),
                                   fertilizer_amount_kg_per_hectare NUMERIC(6, 2),
                                   irrigation_method VARCHAR(50),
                                   notes TEXT,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Weather Data table
CREATE TABLE weather_data (
                              id BIGSERIAL PRIMARY KEY,
                              farm_id BIGINT NOT NULL REFERENCES farms(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                              date DATE NOT NULL,
                              min_temperature NUMERIC(4, 1),
                              max_temperature NUMERIC(4, 1),
                              average_temperature NUMERIC(4, 1),
                              rainfall_mm NUMERIC(6, 2),
                              humidity_percentage NUMERIC(5, 2),
                              wind_speed_kmh NUMERIC(5, 2),
                              solar_radiation NUMERIC(7, 2),
                              source VARCHAR(50) NOT NULL,
                              created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                              UNIQUE (farm_id, date)
);

-- Create Yield History table
CREATE TABLE yield_history (
                               id BIGSERIAL PRIMARY KEY,
                               planting_session_id BIGINT NOT NULL REFERENCES planting_sessions(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                               harvest_date DATE NOT NULL,
                               yield_tons_per_hectare NUMERIC(6, 2) NOT NULL,
                               quality_rating VARCHAR(20),
                               notes TEXT,
                               created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Yield Predictions table
CREATE TABLE yield_predictions (
                                   id BIGSERIAL PRIMARY KEY,
                                   planting_session_id BIGINT NOT NULL REFERENCES planting_sessions(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                                   prediction_date DATE NOT NULL,
                                   predicted_yield_tons_per_hectare NUMERIC(6, 2) NOT NULL,
                                   confidence_percentage NUMERIC(5, 2) NOT NULL,
                                   model_version VARCHAR(50) NOT NULL,
                                   features_used TEXT NOT NULL,
                                   created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                   updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Farm Recommendations table
CREATE TABLE recommendations (
                                 id BIGSERIAL PRIMARY KEY,
                                 planting_session_id BIGINT NOT NULL REFERENCES planting_sessions(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                                 recommendation_date DATE NOT NULL,
                                 category VARCHAR(50) NOT NULL,
                                 title VARCHAR(255) NOT NULL,
                                 description TEXT NOT NULL,
                                 priority VARCHAR(20) NOT NULL,
                                 is_viewed BOOLEAN NOT NULL DEFAULT FALSE,
                                 is_implemented BOOLEAN NOT NULL DEFAULT FALSE,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Pests and Diseases Monitoring table
CREATE TABLE pests_diseases_monitoring (
                                           id BIGSERIAL PRIMARY KEY,
                                           planting_session_id BIGINT NOT NULL REFERENCES planting_sessions(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                                           observation_date DATE NOT NULL,
                                           pest_or_disease_name VARCHAR(100) NOT NULL,
                                           severity_level VARCHAR(20) NOT NULL,
                                           affected_area_percentage NUMERIC(5, 2),
                                           treatment_applied VARCHAR(255),
                                           notes TEXT,
                                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                           updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create Farm Activities table
CREATE TABLE farm_activities (
                                 id BIGSERIAL PRIMARY KEY,
                                 planting_session_id BIGINT NOT NULL REFERENCES planting_sessions(id) ON DELETE CASCADE,  -- Changed from INTEGER to BIGINT
                                 activity_date DATE NOT NULL,
                                 activity_type VARCHAR(50) NOT NULL,
                                 description TEXT NOT NULL,
                                 labor_hours NUMERIC(5, 2),
                                 cost NUMERIC(10, 2),
                                 notes TEXT,
                                 created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                 updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- Create indexes to improve query performance
CREATE INDEX idx_farms_user_id ON farms(user_id);
CREATE INDEX idx_soil_data_farm_id ON soil_data(farm_id);
CREATE INDEX idx_planting_sessions_farm_id ON planting_sessions(farm_id);
CREATE INDEX idx_weather_data_farm_id_date ON weather_data(farm_id, date);
CREATE INDEX idx_yield_history_planting_session_id ON yield_history(planting_session_id);
CREATE INDEX idx_yield_predictions_planting_session_id ON yield_predictions(planting_session_id);
CREATE INDEX idx_recommendations_planting_session_id ON recommendations(planting_session_id);