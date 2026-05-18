INSERT INTO Discipline (Id_Discipline, name) VALUES
(1,'Painting'),
(2,'Sculpture'),
(3,'Photography'),
(4,'Digital Art'),
(5,'Street Art'),
(6,'Ceramics'),
(7,'Illustration'),
(8,'Mixed Media'),
(9,'Performance Art'),
(10,'Installation'),
(11,'Calligraphy'),
(12,'3D Art');

INSERT INTO Tag (Id_Tag, name) VALUES
(1,'Abstract'),
(2,'Minimalist'),
(3,'Urban'),
(4,'Nature'),
(5,'Portrait'),
(6,'Experimental'),
(7,'Colorful'),
(8,'BlackAndWhite'),
(9,'Modern'),
(10,'Surreal'),
(11,'Digital'),
(12,'Handmade'),
(13,'Vintage'),
(14,'Contemporary'),
(15,'Political'),
(16,'Emotional'),
(17,'Geometry'),
(18,'Light'),
(19,'Interactive'),
(20,'Fantasy');

INSERT INTO Artist (Id_Artist, name, bio, birthyear, contactEmail, city, isActive) VALUES
(1,'Lena Moreau','Contemporary painter exploring emotions.',1987,'lena.moreau@artconnect.com','Paris',TRUE),
(2,'Ethan Brooks','Digital artist focused on immersive visuals.',1990,'ethan.brooks@artconnect.com','Berlin',TRUE),
(3,'Sofia Ricci','Italian sculptor inspired by mythology.',1985,'sofia.ricci@artconnect.com','Rome',TRUE),
(4,'Noah Kim','Photographer capturing urban solitude.',1992,'noah.kim@artconnect.com','Seoul',TRUE),
(5,'Mia Laurent','Mixed media artist experimenting with textures.',1994,'mia.laurent@artconnect.com','Lyon',TRUE),
(6,'Lucas Meyer','Minimalist installation artist.',1983,'lucas.meyer@artconnect.com','Amsterdam',TRUE),
(7,'Chloe Martin','Illustrator inspired by dreams.',1991,'chloe.martin@artconnect.com','Brussels',TRUE),
(8,'Oliver Stone','Street artist using political symbolism.',1980,'oliver.stone@artconnect.com','London',TRUE),
(9,'Emma Silva','Ceramic artist focused on organic forms.',1988,'emma.silva@artconnect.com','Lisbon',TRUE),
(10,'Hugo Bernard','Experimental photographer.',1995,'hugo.bernard@artconnect.com','Marseille',TRUE);

INSERT INTO Gallerie (Id_Gallerie, name, adress, rating, ownerName, openingHours, contactPhone, website) VALUES
(1,'Modern Pulse Gallery','12 Rue des Arts, Paris',4.8,'Claire Vincent','10:00-19:00','0102030405','modernpulse.fr'),
(2,'Urban Canvas','45 Alexander Platz, Berlin',4.5,'Mark Hoffman','09:00-18:00','0203040506','urbancanvas.de'),
(3,'Visionary Space','8 Via Roma, Rome',4.7,'Giulia Ferretti','11:00-20:00','0304050607','visionaryspace.it'),
(4,'Neo Gallery','22 Baker Street, London',4.6,'James Carter','10:00-19:00','0405060708','neogallery.uk'),
(5,'LightForm Studio','17 Canal Road, Amsterdam',4.3,'Anna De Vries','09:00-17:00','0506070809','lightform.nl');

INSERT INTO CommunityMember (Id_CommunityMember, name, email, birthyear, phone, membershipType, city) VALUES
(1,'Alice Bernard','alice@mail.com',1998,'0600000001','FREE','Paris'),
(2,'Tom Rivera','tom@mail.com',1995,'0600000002','PREMIUM','Berlin'),
(3,'Emma White','emma@mail.com',1990,'0600000003','FREE','Rome'),
(4,'Luca Rossi','luca@mail.com',1988,'0600000004','PREMIUM','Milan'),
(5,'Mila Carter','mila@mail.com',1997,'0600000005','FREE','London'),
(6,'Nathan Cole','nathan@mail.com',1992,'0600000006','PREMIUM','Paris'),
(7,'Olivia Diaz','olivia@mail.com',1999,'0600000007','FREE','Madrid'),
(8,'Leo Martin','leo@mail.com',1993,'0600000008','PREMIUM','Brussels');

INSERT INTO Exhibition (Id_Exhibition, title, start_date, end_date, Id_Gallerie, theme, description, curatorName) VALUES
(1,'Urban Echoes','2026-02-01','2026-03-15',1,'City and Isolation','Exploration of urban loneliness','Claire Vincent'),
(2,'Digital Horizons','2026-04-10','2026-05-30',2,'Future Technology','Digital transformation in art','Mark Hoffman'),
(3,'Nature Reimagined','2026-06-01','2026-07-15',3,'Ecology','Organic and natural forms','Giulia Ferretti'),
(4,'Dream Layers','2026-08-01','2026-09-10',4,'Surrealism','Dream-inspired creations','James Carter');

INSERT INTO Workshop (Id_Workshop, title, date_, price, Id_Artist, level, durationMinutes, maxParticipants, location, description) VALUES
(1,'Introduction to Abstract Painting','2026-06-15 14:00:00',45.00,1,'BEGINNER',120,15,'Paris','Learn abstract techniques'),
(2,'Street Art Basics','2026-06-20 16:00:00',35.00,8,'BEGINNER',90,20,'Berlin','Discover urban expression'),
(3,'Digital Illustration Masterclass','2026-07-01 10:00:00',80.00,2,'ADVANCED',180,10,'London','Advanced digital workflows'),
(4,'Ceramic Sculpture Workshop','2026-07-10 13:00:00',60.00,9,'INTERMEDIATE',150,12,'Lisbon','Create ceramic forms');

INSERT INTO Artwork (Id_Artwork, title, creationYear, type, price, Id_Artist, status, medium, dimensions, description, Id_Exhibition) VALUES
(1,'Silent Geometry',2025,'Painting',1200.00,1,'FOR_SALE','Oil on canvas','100x80cm','Abstract geometric composition',1),
(2,'Neon Streets',2026,'Digital Art',2200.00,2,'EXHIBITED','Digital print','1920x1080','Cyberpunk inspired cityscape',2),
(3,'Roots of Memory',2024,'Sculpture',3400.00,3,'SOLD','Clay','70x40cm','Organic sculptural piece',3),
(4,'Parallel Dreams',2026,'Illustration',850.00,7,'FOR_SALE','Ink and watercolor','50x70cm','Dreamlike illustration',4),
(5,'Invisible Faces',2025,'Photography',1500.00,4,'EXHIBITED','Photography print','60x90cm','Urban portrait series',1),
(6,'Broken Signals',2025,'Street Art',2000.00,8,'FOR_SALE','Spray paint','200x150cm','Political street composition',1);

INSERT INTO qualified (Id_Artwork, Id_Tag) VALUES
(1,1),(1,17),(1,14),
(2,11),(2,20),(2,9),
(3,4),(3,12),
(4,10),(4,16),
(5,5),(5,8),
(6,3),(6,15);

INSERT INTO master (Id_Artist, Id_Discipline) VALUES
(1,1),
(1,8),
(2,4),
(2,12),
(3,2),
(4,3),
(5,8),
(6,10),
(7,7),
(8,5),
(9,6),
(10,3);

INSERT INTO Review (Id_Review, Id_CommunityMember, Id_Artwork, rating, comment, reviewDate) VALUES
(1,1,1,5,'Amazing atmosphere and details','2026-03-01'),
(2,2,2,4,'Very immersive artwork','2026-03-02'),
(3,3,3,5,'Beautiful textures and concept','2026-03-05'),
(4,4,4,3,'Interesting but difficult to understand','2026-03-06'),
(5,5,5,5,'One of the best exhibitions this year','2026-03-08'),
(6,6,6,4,'Strong political message','2026-03-10');

INSERT INTO booking (Id_Workshop, Id_CommunityMember, bookingDate, paymentStatus) VALUES
(1,1,'2026-05-20 10:00:00','PAID'),
(1,2,'2026-05-21 14:00:00','PAID'),
(2,3,'2026-05-22 15:00:00','PENDING'),
(3,4,'2026-05-25 18:00:00','PAID'),
(4,5,'2026-05-28 09:00:00','CANCELLED');

INSERT INTO favoriteDisciplines (Id_CommunityMember, Id_Discipline) VALUES
(1,1),
(1,7),
(2,4),
(2,12),
(3,2),
(3,6),
(4,5),
(5,3),
(5,8),
(6,1),
(6,10),
(7,9),
(8,4);