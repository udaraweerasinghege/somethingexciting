-- Steady work

SET SEARCH_PATH TO markus;
DROP TABLE IF EXISTS groupida1 CASCADE;
DROP TABLE IF EXISTS firstfile CASCADE;
DROP TABLE IF EXISTS firstfilewithuser CASCADE;
DROP TABLE IF EXISTS lastfile CASCADE;
DROP TABLE IF EXISTS lastfilewithuser CASCADE;
DROP TABLE IF EXISTS elapsedtime CASCADE;
DROP TABLE IF EXISTS q6;

-- You must not change this table definition.
CREATE TABLE groupida1(
  group_id int
);
CREATE TABLE firstfile(
  group_id int,
  first_file VARCHAR(20),
  first_time timestamp
);
CREATE TABLE firstfilewithuser(
  group_id int,
  first_file VARCHAR(20),
  first_time timestamp,
  first_submitter VARCHAR(20)
);
CREATE TABLE lastfile(
  group_id int,
  last_file VARCHAR(20),
  last_time timestamp
);
CREATE TABLE lastfilewithuser(
  group_id int,
  last_file VARCHAR(20),
  last_time timestamp,
  last_submitter VARCHAR(20)
);
CREATE TABLE elapsedtime(
  group_id int,
  first_time timestamp,
  last_time timestamp,
  elapsed_time interval
);
CREATE TABLE q6 (
	group_id integer,
	first_file varchar(25),
	first_time timestamp,
	first_submitter varchar(25),
	last_file varchar(25),
	last_time timestamp,
	last_submitter varchar(25),
	elapsed_time interval
);

--The group_id of all the groups that completed a1
INSERT INTO groupida1(group_id)
SELECT group_id
FROM assignmentgroup NATURAL JOIN assignment
WHERE description = 'a1';

-- The tuple containing information about the first file submitted by a group
-- for a1
INSERT INTO firstfile (group_id, first_file, first_time)
SELECT group_id, file_name, min(submission_date)
FROM groupida1 NATURAL JOIN submissions
GROUP BY group_id, file_name;

-- The relation containing information about the first file submitted by a group
--for a1, including username of the person who submitted it
INSERT INTO firstfilewithuser (group_id, first_file, first_time, first_submitter)
SELECT firstfile.group_id, file_name, submission_date, username
FROM submissions JOIN firstfile
ON submission_date = first_time
AND file_name = first_file;

-- The tuple containing information about the last file submitted by a group
-- for a1
INSERT INTO lastfile (group_id, last_file, last_time)
SELECT group_id, file_name, max(submission_date)
FROM groupida1 NATURAL JOIN submissions
GROUP BY group_id, file_name;

-- The relation containing information about the last file submitted by a group
--for a1, including username of the person who submitted it
INSERT INTO lastfilewithuser (group_id, last_file, last_time, last_submitter)
SELECT lastfile.group_id, file_name, submission_date, username
FROM submissions JOIN lastfile
ON submission_date = last_time
AND file_name = last_file;

-- The elapsed time from the first and last submission
INSERT INTO elapsedtime(group_id, first_time, last_time, elapsed_time)
SELECT firstfile.group_id, first_time, last_time, last_time::timestamp - first_time::timestamp
FROM firstfile JOIN lastfile ON firstfile.group_id = lastfile.group_id;

-- The relation containing the answer
INSERT INTO q6
SELECT firstfilewithuser.group_id,
    firstfilewithuser.first_file, firstfilewithuser.first_time, firstfilewithuser.first_submitter,
    lastfilewithuser.last_file, lastfilewithuser.last_time,
    lastfilewithuser.last_submitter, elapsed_time
FROM firstfilewithuser JOIN lastfilewithuser
ON firstfilewithuser.first_file = lastfilewithuser.last_file
JOIN elapsedtime on elapsedtime.group_id = firstfilewithuser.group_id;
