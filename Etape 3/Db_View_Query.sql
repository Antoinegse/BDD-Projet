CREATE VIEW vw_artworks_for_sale AS
SELECT
    a.Id_Artwork,
    a.title,
    a.price,
    a.type,
    a.medium,
    ar.name AS artist_name
FROM Artwork a
JOIN Artist ar ON a.Id_Artist = ar.Id_Artist
WHERE a.status = 'FOR_SALE';

CREATE VIEW vw_workshop_participants AS
SELECT
    w.Id_Workshop,
    w.title,
    w.maxParticipants,
    COUNT(b.Id_CommunityMember) AS nbParticipants
FROM Workshop w
LEFT JOIN booking b
ON w.Id_Workshop = b.Id_Workshop
GROUP BY w.Id_Workshop;

CREATE VIEW vw_public_members AS
SELECT
    Id_CommunityMember,
    name,
    city,
    membershipType
FROM CommunityMember;