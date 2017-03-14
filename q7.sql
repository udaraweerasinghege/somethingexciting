-- High coverage

SET SEARCH_PATH TO markus;
DROP TABLE IF EXISTS allassignments CASCADE;
DROP TABLE IF EXISTS assignedall CASCADE;
DROP TABLE IF EXISTS allmarked CASCADE;
DROP TABLE IF EXISTS q7;

-- You must not change this table definition.
CREATE TABLE q7 (
	ta varchar(100)
);
CREATE TABLE allassignments(
  assignment int,
  ta VARCHAR(30)
);
CREATE TABLE assignedall(
  ta VARCHAR(30)
);
CREATE TABLE allmarked(
  ta VARCHAR(30)
);

-- Relation with all assignment ids and the TAs who are assigned to mark the
-- assignments
INSERT INTO allassignments(assignment, ta)
SELECT assignment.assignment_id, username
FROM assignmentgroup JOIN assignment
ON assignmentgroup.assignment_id = assignment.assignment_id
LEFT JOIN grader ON assignmentgroup.group_id = grader.group_id;


-- Username of all graders who have been assigned at least one group for every
-- assignment
INSERT INTO assignedall(ta)
SELECT ta
FROM allassignments
GROUP BY ta
HAVING COUNT(DISTINCT assignment) >= (SELECT COUNT(DISTINCT assignment_id)
                    FROM assignment);

-- username of all TAs who have marked a submission for every assignment
INSERT INTO allmarked(ta)
SELECT grader.username
FROM membership JOIN grader ON membership.group_id = grader.group_id
GROUP BY grader.username
HAVING (SELECT COUNT(DISTINCT username)
        FROM membership) = (SELECT COUNT(DISTINCT membership.username)
                            FROM membership JOIN grader
                            ON membership.group_id = grader.group_id
                            GROUP BY grader.username);
-- Final answer.
INSERT INTO q7
SELECT allmarked.ta
FROM allmarked NATURAL JOIN assignedall;
