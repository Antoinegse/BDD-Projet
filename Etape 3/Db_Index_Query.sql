CREATE INDEX idx_artwork_title
ON Artwork(title);

CREATE INDEX idx_exhibition_dates
ON Exhibition(start_date, end_date);

CREATE INDEX idx_booking_workshop
ON booking(Id_Workshop);