CREATE USER 'visitor'@'localhost'
IDENTIFIED BY 'visitor123';

GRANT SELECT
ON artconnect.vw_artworks_for_sale
TO 'visitor'@'localhost';

GRANT SELECT
ON artconnect.vw_public_members
TO 'visitor'@'localhost';