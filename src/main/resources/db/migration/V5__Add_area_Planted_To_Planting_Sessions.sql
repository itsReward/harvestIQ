-- Add areaPlanted column to planting_sessions table
ALTER TABLE planting_sessions 
ADD COLUMN area_planted NUMERIC(10, 2);

-- Add comment to describe the column
COMMENT ON COLUMN planting_sessions.area_planted IS 'Area planted in hectares for this planting session';