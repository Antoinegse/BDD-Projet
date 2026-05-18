START TRANSACTION;

INSERT INTO booking(Id_Workshop, Id_CommunityMember)
VALUES (1, 5);

INSERT INTO booking(Id_Workshop, Id_CommunityMember)
VALUES (2, 5);

COMMIT;