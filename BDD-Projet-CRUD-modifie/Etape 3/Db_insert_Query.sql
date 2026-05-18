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

INSERT INTO Artist (Id_Artist, name, city, contactEmail, birthyear, bio, isActive) VALUES
(1,'Lena Moreau','Paris','lena.moreau@artconnect.com',1987,'Contemporary painter exploring emotions.',TRUE),
(2,'Ethan Brooks','Berlin','ethan.brooks@artconnect.com',1990,'Digital artist focused on immersive visuals.',TRUE),
(3,'Sofia Ricci','Rome','sofia.ricci@artconnect.com',1985,'Italian sculptor inspired by mythology.',TRUE),
(4,'Noah Kim','Seoul','noah.kim@artconnect.com',1992,'Photographer capturing urban solitude.',TRUE),
(5,'Mia Laurent','Lyon','mia.laurent@artconnect.com',1994,'Mixed media artist experimenting with textures.',TRUE),
(6,'Lucas Meyer','Amsterdam','lucas.meyer@artconnect.com',1983,'Minimalist installation artist.',TRUE),
(7,'Chloe Martin','Brussels','chloe.martin@artconnect.com',1991,'Illustrator inspired by dreams.',TRUE),
(8,'Oliver Stone','London','oliver.stone@artconnect.com',1980,'Street artist using political symbolism.',TRUE),
(9,'Emma Silva','Lisbon','emma.silva@artconnect.com',1988,'Ceramic artist focused on organic forms.',TRUE),
(10,'Hugo Bernard','Marseille','hugo.bernard@artconnect.com',1995,'Experimental photographer.',TRUE),
(11,'Ava Turner','New York','ava.turner@artconnect.com',1986,'Digital surrealist artist.',TRUE),
(12,'Leo Fischer','Munich','leo.fischer@artconnect.com',1984,'Painter combining geometry and abstraction.',TRUE),
(13,'Nina Costa','Barcelona','nina.costa@artconnect.com',1993,'Performance artist exploring identity.',TRUE),
(14,'Daniel Novak','Prague','daniel.novak@artconnect.com',1982,'Interactive installation creator.',TRUE),
(15,'Sarah Dupont','Paris','sarah.dupont@artconnect.com',1996,'Young contemporary illustrator.',TRUE);

INSERT INTO Gallerie (Id_Gallerie, name, adress, rating, ownerName, openingHours, contactPhone, website) VALUES
(1,'Modern Pulse Gallery','Paris','12 Rue des Arts',4.8,'Claire Vincent','10:00-19:00','0102030405','modernpulse.fr'),
(2,'Urban Canvas','Berlin','45 Alexander Platz',4.5,'Mark Hoffman','09:00-18:00','0203040506','urbancanvas.de'),
(3,'Visionary Space','Rome','8 Via Roma',4.7,'Giulia Ferretti','11:00-20:00','0304050607','visionaryspace.it'),
(4,'Neo Gallery','London','22 Baker Street',4.6,'James Carter','10:00-19:00','0405060708','neogallery.uk'),
(5,'LightForm Studio','Amsterdam','17 Canal Road',4.3,'Anna De Vries','09:00-17:00','0506070809','lightform.nl'),
(6,'Art District','Barcelona','9 Avenida Sol',4.4,'Luis Ortega','10:00-21:00','0607080910','artdistrict.es'),
(7,'Creative Hub','Brussels','33 Central Ave',4.2,'Elise Martin','10:00-18:00','0708091011','creativehub.be'),
(8,'Digital Dreams','Seoul','88 Gangnam Ave',4.9,'Min Jae','12:00-22:00','0809101112','digitaldreams.kr'),
(9,'Abstract Corner','Lisbon','15 Ocean Street',4.1,'Ricardo Lopes','09:00-18:00','0910111213','abstractcorner.pt'),
(10,'Future Arts','New York','90 Madison Ave',4.8,'Olivia Green','10:00-20:00','1011121314','futurearts.us');

INSERT INTO CommunityMember (Id_CommunityMember, birthyear, phone, membershipType, name, email, city) VALUES
(1,1998,'0600000001','FREE','Alice Bernard','alice@mail.com','Paris'),
(2,1995,'0600000002','PREMIUM','Tom Rivera','tom@mail.com','Berlin'),
(3,1990,'0600000003','FREE','Emma White','emma@mail.com','Rome'),
(4,1988,'0600000004','PREMIUM','Luca Rossi','luca@mail.com','Milan'),
(5,1997,'0600000005','FREE','Mila Carter','mila@mail.com','London'),
(6,1992,'0600000006','PREMIUM','Nathan Cole','nathan@mail.com','Paris'),
(7,1999,'0600000007','FREE','Olivia Diaz','olivia@mail.com','Madrid'),
(8,1993,'0600000008','PREMIUM','Leo Martin','leo@mail.com','Brussels'),
(9,1991,'0600000009','FREE','Sophia Reed','sophia@mail.com','Lisbon'),
(10,1987,'0600000010','PREMIUM','Ethan Scott','ethan@mail.com','Amsterdam');

INSERT INTO Exhibition (Id_Exhibition, title, start_date, end_date, theme, description, curatorName, Id_Gallerie) VALUES
(1,'Urban Echoes','2026-02-01','2026-03-15','City and Isolation','Exploration of urban loneliness','Claire Vincent',1),
(2,'Digital Horizons','2026-04-10','2026-05-30','Future Technology','Digital transformation in art','Min Jae',8),
(3,'Nature Reimagined','2026-06-01','2026-07-15','Ecology','Organic and natural forms','Luis Ortega',6),
(4,'Dream Layers','2026-08-01','2026-09-10','Surrealism','Dream-inspired creations','James Carter',4),
(5,'Fragments of Identity','2026-10-05','2026-11-20','Identity','Exploration of self-image','Olivia Green',10);

INSERT INTO Workshop (Id_Workshop, title, date_, price, level, durationMinutes, maxParticipants, location, description, Id_Artist) VALUES
(1,'Introduction to Abstract Painting','2026-06-15 14:00:00',45.00,'BEGINNER',120,15,'Paris','Learn abstract techniques',1),
(2,'Street Art Basics','2026-06-20 16:00:00',35.00,'BEGINNER',90,20,'Berlin','Discover urban expression',8),
(3,'Digital Illustration Masterclass','2026-07-01 10:00:00',80.00,'ADVANCED',180,10,'London','Advanced digital workflows',11),
(4,'Ceramic Sculpture Workshop','2026-07-10 13:00:00',60.00,'INTERMEDIATE',150,12,'Lisbon','Create ceramic forms',9),
(5,'Photography and Emotion','2026-07-15 11:00:00',50.00,'INTERMEDIATE',120,18,'Seoul','Capture emotional scenes',4);

INSERT INTO Artwork (Id_Artwork, title, type, status, price, creationYear, medium, dimensions, description, Id_Exhibition, Id_Artist) VALUES
(1,'Silent Geometry','Painting','FOR_SALE',1200.00,2025,'Oil on canvas','100x80cm','Abstract geometric composition',1,12),
(2,'Neon Streets','Digital Art','EXHIBITED',2200.00,2026,'Digital print','1920x1080','Cyberpunk inspired cityscape',2,2),
(3,'Roots of Memory','Sculpture','SOLD',3400.00,2024,'Clay','70x40cm','Organic sculptural piece',3,3),
(4,'Parallel Dreams','Illustration','FOR_SALE',850.00,2026,'Ink and watercolor','50x70cm','Dreamlike illustration',4,7),
(5,'Invisible Faces','Photography','EXHIBITED',1500.00,2025,'Photography print','60x90cm','Urban portrait series',1,4),
(6,'Broken Signals','Street Art','FOR_SALE',2000.00,2025,'Spray paint','200x150cm','Political street composition',1,8),
(7,'Digital Bloom','Digital Art','FOR_SALE',1750.00,2026,'3D render','4K format','Floral digital experiment',2,11),
(8,'Ocean Fragments','Ceramics','SOLD',950.00,2024,'Ceramic','40x30cm','Sea inspired ceramic work',3,9);

INSERT INTO qualified (Id_Artwork, Id_Tag) VALUES
(1,1),(1,17),(1,14),
(2,11),(2,20),(2,9),
(3,4),(3,12),
(4,10),(4,16),
(5,5),(5,8),
(6,3),(6,15),
(7,11),(7,7),(7,19),
(8,4),(8,12);

INSERT INTO master (Id_Artist, Id_Discipline) VALUES
(1,1),(1,8),
(2,4),(2,12),
(3,2),
(4,3),
(5,8),
(6,10),
(7,7),
(8,5),
(9,6),
(10,3),
(11,4),
(12,1),
(13,9),
(14,10),
(15,7);

INSERT INTO Review (Id_Review, rating, comment, reviewDate, Id_Artwork, Id_CommunityMember) VALUES
(1,5,'Amazing atmosphere and details','2026-03-01',1,1),
(2,4,'Very immersive artwork','2026-03-02',2,2),
(3,5,'Beautiful textures and concept','2026-03-05',3,3),
(4,3,'Interesting but difficult to understand','2026-03-06',4,4),
(5,5,'One of the best exhibitions this year','2026-03-08',5,5),
(6,4,'Strong political message','2026-03-10',6,6),
(7,5,'Incredible use of digital media','2026-03-11',7,7),
(8,4,'Elegant ceramic work','2026-03-12',8,8);

INSERT INTO booking (Id_Workshop, Id_CommunityMember, bookingDate, paymentStatus) VALUES
(1,1,'2026-05-20 10:00:00','PAID'),
(1,2,'2026-05-21 14:00:00','PAID'),
(2,3,'2026-05-22 15:00:00','PENDING'),
(3,4,'2026-05-25 18:00:00','PAID'),
(4,5,'2026-05-28 09:00:00','CANCELLED'),
(5,6,'2026-05-29 12:00:00','PAID');

INSERT INTO favoriteDisciplines (Id_CommunityMember, Id_Discipline) VALUES
(1,1),(1,7),
(2,4),(2,12),
(3,2),(3,6),
(4,5),
(5,3),(5,8),
(6,1),(6,10),
(7,9),
(8,4),(8,11),
(9,6),
(10,2),(10,7);