CREATE INDEX IF NOT EXISTS idx_spots_location ON spots USING GIST(location);
