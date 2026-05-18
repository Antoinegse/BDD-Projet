DELIMITER //

CREATE PROCEDURE add_artwork(
    IN p_title VARCHAR(255),
    IN p_year INT,
    IN p_type VARCHAR(50),
    IN p_price DECIMAL(10,2),
    IN p_artist INT
)
BEGIN

    INSERT INTO Artwork(
        title,
        creationYear,
        type,
        price,
        Id_Artist
    )
    VALUES(
        p_title,
        p_year,
        p_type,
        p_price,
        p_artist
    );

END //

DELIMITER ;

DELIMITER //

CREATE PROCEDURE register_member(
    IN p_workshop INT,
    IN p_member INT
)
BEGIN

    INSERT INTO booking(
        Id_Workshop,
        Id_CommunityMember
    )
    VALUES(
        p_workshop,
        p_member
    );

END //

DELIMITER ;

DELIMITER //

CREATE PROCEDURE create_exhibition(
    IN p_title VARCHAR(255),
    IN p_start DATE,
    IN p_end DATE,
    IN p_gallery INT
)
BEGIN

    INSERT INTO Exhibition(
        title,
        start_date,
        end_date,
        Id_Gallerie
    )
    VALUES(
        p_title,
        p_start,
        p_end,
        p_gallery
    );

END //

DELIMITER ;