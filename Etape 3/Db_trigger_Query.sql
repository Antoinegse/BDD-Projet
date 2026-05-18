DELIMITER //

CREATE TRIGGER trg_check_max_participants
BEFORE INSERT ON booking
FOR EACH ROW
BEGIN

    DECLARE nb INT;
    DECLARE maxP INT;

    SELECT COUNT(*)
    INTO nb
    FROM booking
    WHERE Id_Workshop = NEW.Id_Workshop;

    SELECT maxParticipants
    INTO maxP
    FROM Workshop
    WHERE Id_Workshop = NEW.Id_Workshop;

    IF nb >= maxP THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Workshop complet';
    END IF;

END //

DELIMITER ;

DELIMITER //

CREATE TRIGGER trg_check_exhibition_dates
BEFORE INSERT ON Exhibition
FOR EACH ROW
BEGIN

    IF NEW.end_date < NEW.start_date THEN
        SIGNAL SQLSTATE '45000'
        SET MESSAGE_TEXT = 'Date de fin invalide';
    END IF;

END //

DELIMITER ;

CREATE TABLE ArtworkAudit (
    Id_Audit INT AUTO_INCREMENT PRIMARY KEY,
    Id_Artwork INT,
    oldPrice DECIMAL(10,2),
    newPrice DECIMAL(10,2),
    modificationDate DATETIME DEFAULT CURRENT_TIMESTAMP
);

DELIMITER //

CREATE TRIGGER trg_artwork_price_update
AFTER UPDATE ON Artwork
FOR EACH ROW
BEGIN

    IF OLD.price <> NEW.price THEN

        INSERT INTO ArtworkAudit(
            Id_Artwork,
            oldPrice,
            newPrice
        )
        VALUES(
            OLD.Id_Artwork,
            OLD.price,
            NEW.price
        );

    END IF;

END //

DELIMITER ;