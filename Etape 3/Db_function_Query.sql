DELIMITER //

CREATE FUNCTION get_participants_count(
    p_workshop INT
)
RETURNS INT
DETERMINISTIC
BEGIN

    DECLARE total INT;

    SELECT COUNT(*)
    INTO total
    FROM booking
    WHERE Id_Workshop = p_workshop;

    RETURN total;

END //

DELIMITER ;

DELIMITER //

CREATE FUNCTION avg_artist_price(
    p_artist INT
)
RETURNS DECIMAL(10,2)
DETERMINISTIC
BEGIN

    DECLARE avgPrice DECIMAL(10,2);

    SELECT AVG(price)
    INTO avgPrice
    FROM Artwork
    WHERE Id_Artist = p_artist;

    RETURN avgPrice;

END //

DELIMITER ;

DELIMITER //

CREATE FUNCTION sold_artworks_count()
RETURNS INT
DETERMINISTIC
BEGIN

    DECLARE total INT;

    SELECT COUNT(*)
    INTO total
    FROM Artwork
    WHERE status = 'SOLD';

    RETURN total;

END //

DELIMITER ;