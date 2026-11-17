-- User information
INSERT INTO Users (user_id, first_name, last_name, year_of_birth, month_of_birth, day_of_birth, gender)
SELECT DISTINCT user_id, first_name, last_name, year_of_birth, month_of_birth, day_of_birth, gender
FROM project1.Public_User_Information;


-- Friends
INSERT INTO Friends (user1_id, user2_id)
SELECT DISTINCT 
    LEAST(user1_id, user2_id) AS user1_id, 
    GREATEST(user1_id, user2_id) AS user2_id
FROM project1.Public_Are_Friends
WHERE user1_id IS NOT NULL AND user2_id IS NOT NULL
AND user1_id != user2_id; 

-- city information
INSERT INTO Cities (city_name, state_name, country_name)
SELECT current_city, current_state, current_country
FROM project1.Public_User_Information
WHERE current_city IS NOT NULL
UNION
SELECT hometown_city, hometown_state, hometown_country
FROM project1.Public_User_Information
WHERE hometown_city IS NOT NULL
UNION
SELECT event_city, event_state, event_country
FROM project1.Public_Event_Information
WHERE event_city IS NOT NULL;


-- User_Current_Cities
INSERT INTO User_Current_Cities (user_id, current_city_id)
SELECT DISTINCT P.user_id, C.city_id
FROM project1.Public_User_Information P
JOIN Cities C
  ON P.current_city = C.city_name
 AND P.current_state = C.state_name
 AND P.current_country = C.country_name;


-- User_Hometown_Cities
INSERT INTO User_Hometown_Cities (user_id, hometown_city_id)
SELECT DISTINCT P.user_id, C.city_id
FROM project1.Public_User_Information P
JOIN Cities C
  ON P.hometown_city = C.city_name
 AND P.hometown_state = C.state_name
 AND P.hometown_country = C.country_name;


-- Messages (No data)


-- program information
INSERT INTO Programs (institution, concentration, degree)
SELECT DISTINCT institution_name, program_concentration, program_degree
FROM project1.Public_User_Information
WHERE institution_name IS NOT NULL;


-- Education
INSERT INTO Education (user_id, program_id, program_year)
SELECT DISTINCT P.user_id, Prog.program_id, P.program_year
FROM project1.Public_User_Information P
JOIN Programs Prog
  ON P.institution_name = Prog.institution
 AND P.program_concentration = Prog.concentration
 AND P.program_degree = Prog.degree
WHERE P.institution_name IS NOT NULL;


-- Event
INSERT INTO User_Events (
    event_id, event_creator_id, event_name, event_tagline, 
    event_description, event_host, event_type, event_subtype, 
    event_address, event_city_id, event_start_time, event_end_time
)
SELECT DISTINCT 
    P.event_id, P.event_creator_id, P.event_name, P.event_tagline,
    P.event_description, P.event_host, P.event_type, P.event_subtype,
    P.event_address, C.city_id, P.event_start_time, P.event_end_time
FROM project1.Public_Event_Information P
JOIN Cities C
  ON P.event_city = C.city_name
 AND P.event_state = C.state_name
 AND P.event_country = C.country_name;


-- Participation (No data)
SET AUTOCOMMIT OFF;

-- Album
INSERT INTO Albums (
    album_id, album_owner_id, album_name, album_created_time, 
    album_modified_time, album_link, album_visibility, cover_photo_id
)
SELECT 
    album_id, owner_id, album_name, album_created_time,
    MAX(album_modified_time), album_link, album_visibility, cover_photo_id
FROM project1.Public_Photo_Information
GROUP BY 
    album_id, owner_id, album_name, album_created_time, 
    album_link, album_visibility, cover_photo_id;


-- Photos
INSERT INTO Photos (photo_id, album_id, photo_caption, photo_created_time, photo_modified_time, photo_link)
SELECT DISTINCT photo_id, album_id, photo_caption, photo_created_time, photo_modified_time, photo_link
FROM project1.Public_Photo_Information;

COMMIT;
SET AUTOCOMMIT ON;

-- Tag
INSERT INTO Tags (tag_photo_id, tag_subject_id, tag_created_time, tag_x, tag_y)
SELECT DISTINCT photo_id, tag_subject_id, tag_created_time, tag_x_coordinate, tag_y_coordinate
FROM project1.Public_Tag_Information;

