START TRANSACTION;

INSERT INTO booking(Id_Workshop, Id_CommunityMember)
VALUES (1, 3);

INSERT INTO booking(Id_Workshop, Id_CommunityMember)
VALUES (2, 3);

INSERT INTO booking(Id_Workshop, Id_CommunityMember)
VALUES (5, 3);

COMMIT;

DELIMITER //

CREATE PROCEDURE multi_booking()
BEGIN

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
    END;

    START TRANSACTION;

    INSERT INTO booking VALUES(1,3,NOW(),'PAID');
    INSERT INTO booking VALUES(2,3,NOW(),'PAID');
    INSERT INTO booking VALUES(5,3,NOW(),'PAID');

    COMMIT;

END //

DELIMITER ;