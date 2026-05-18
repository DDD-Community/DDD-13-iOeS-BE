CREATE INDEX IF NOT EXISTS idx_spots_location ON spots USING GIST(location);
CREATE INDEX IF NOT EXISTS idx_spots_user_id ON spots(user_id);
