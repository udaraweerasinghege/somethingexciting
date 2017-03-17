import java.sql.*;

//Remember that part of your mark is for doing as much in SQL (not Java)
//as you can. At most you can justify using an array, or the more flexible
//ArrayList. Don't go crazy with it, though. You need it rarely if at all.
import java.util.ArrayList;

public class Assignment2 {

	// A connection to the database
	Connection connection;

	Assignment2() throws SQLException {
		try {
			Class.forName("org.postgresql.Driver");
		} catch (ClassNotFoundException e) {
			e.printStackTrace();
		}
	}

	/**
	 * Connects to the database and sets the search path.
	 *
	 * Establishes a connection to be used for this session, assigning it to the
	 * instance variable 'connection'. In addition, sets the search path to
	 * markus.
	 *
	 * @param url
	 *            the url for the database
	 * @param username
	 *            the username to be used to connect to the database
	 * @param password
	 *            the password to be used to connect to the database
	 * @return true if connecting is successful, false otherwise
	 */
	public boolean connectDB(String URL, String username, String password) {
		try {
			connection = DriverManager.getConnection(URL, username, password);
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Closes the database connection.
	 *
	 * @return true if the closing was successful, false otherwise
	 */
	public boolean disconnectDB() {
		try {
			connection.close();
		} catch (Exception e) {
			return false;
		}
		return true;
	}

	/**
	 * Assigns a grader for a group for an assignment.
	 *
	 * Returns false if the groupID does not exist in the AssignmentGroup table,
	 * if some grader has already been assigned to the group, or if grader is
	 * not either a TA or instructor.
	 *
	 * @param groupID
	 *            id of the group
	 * @param grader
	 *            username of the grader
	 * @return true if the operation was successful, false otherwise
	 * @throws SQLException
	 */
	public boolean assignGrader(int groupID, String grader) {
		String queryString = "SET search_path TO markus";
		PreparedStatement pStatement;

		try {
			pStatement = connection.prepareStatement(queryString);
			pStatement.execute();
			ResultSet rs;
			// If grader is not valid
			queryString = "SELECT type FROM markususer WHERE username = '" + grader + "'";
			PreparedStatement userSearch = connection.prepareStatement(queryString);
			rs = userSearch.executeQuery();
			if (rs.next()) {
				String graderStatus = rs.getString("type");
				if (!graderStatus.trim().equals("TA") && !graderStatus.trim().equals("instructor")) {
					return false;
				}
			} else {
				return false;
			}
			// If group does not exist
			String searchGroup = "SELECT group_id from assignmentgroup WHERE group_id = " + groupID;
			PreparedStatement groupSearch;
			groupSearch = connection.prepareStatement(searchGroup);
			ResultSet rs1;
			rs1 = groupSearch.executeQuery();
			if (!rs1.next()) {
				return false;
			}
			// Check if group is already assigned
			queryString = "SELECT username, group_id FROM grader WHERE group_id = " + groupID;
			groupSearch = connection.prepareStatement(queryString);
			rs1 = groupSearch.executeQuery();
			if (rs1.next()) {
				String username = rs1.getString("username");
				if (username != grader) {
					return false;
				}
			} else {
				String insertTable = "INSERT INTO grader " + "(group_id, username) VALUES" + "(?,?)";
				PreparedStatement preparedS = connection.prepareStatement(insertTable);
				preparedS.setInt(1, groupID);
				preparedS.setString(2, grader);
				preparedS.executeUpdate();
				return true;
			}
		} catch (

		SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Adds a member to a group for an assignment.
	 *
	 * Records the fact that a new member is part of a group for an assignment.
	 * Does nothing (but returns true) if the member is already declared to be
	 * in the group.
	 *
	 * Does nothing and returns false if any of these conditions hold: - the
	 * group is already at capacity, - newMember is not a valid username or is
	 * not a student, - there is no assignment with this assignment ID, or - the
	 * group ID has not been declared for the assignment.
	 *
	 * @param assignmentID
	 *            id of the assignment
	 * @param groupID
	 *            id of the group to receive a new member
	 * @param newMember
	 *            username of the new member to be added to the group
	 * @return true if the operation was successful, false otherwise
	 */
	public boolean recordMember(int assignmentID, int groupID, String newMember) {
		String queryString = "SET search_path TO markus";
		PreparedStatement pStatement;
		try {
			pStatement = connection.prepareStatement(queryString);
			ResultSet rs;
			ResultSet rs1;
			ResultSet rs2;
			pStatement.execute();
			// Check if group is at max
			queryString = "select count(membership.username), membership.group_id, assignment.group_max from membership join assignmentgroup on assignmentgroup.group_id = membership.group_id join assignment on assignment.assignment_id = assignmentgroup.assignment_id join markususer on markususer.username = membership.username where assignmentgroup.group_id = "
					+ groupID + " group by membership.group_id, assignment.group_max";
			PreparedStatement ps = connection.prepareStatement(queryString);
			rs = ps.executeQuery();
			if (rs.next()) {
				int curr_num_members = rs.getInt("count");
				int max_members = rs.getInt("group_max");
				if (max_members == curr_num_members) {
					return false;
				}
			}
			// Check if assignment exists
			queryString = "SELECT assignment_id FROM assignment WHERE assignment_id = " + assignmentID;
			PreparedStatement ps1 = connection.prepareStatement(queryString);
			rs1 = ps1.executeQuery();
			if (!rs1.next()) {
				return false;
			}
			// Check if group is valid
			queryString = "SELECT group_id from assignmentgroup WHERE group_id = " + groupID;
			PreparedStatement searchGroup = connection.prepareStatement(queryString);
			ResultSet searchedGroup;
			searchedGroup = searchGroup.executeQuery();
			if (!searchedGroup.next()) {
				return false;
			}
			// Check if student is valid
			queryString = "SELECT username from markususer where username = '" + newMember + "'";
			PreparedStatement searchUser = connection.prepareStatement(queryString);
			ResultSet searchedUser;
			searchedUser = searchUser.executeQuery();
			if (!searchedUser.next()) {
				return false;
			}
			queryString = "select username from markususer where username = '" + newMember + "' and type = 'student'";
			PreparedStatement ps2 = connection.prepareStatement(queryString);
			rs2 = ps2.executeQuery();
			if (!rs2.next()) {
				return false;
			}
			String search = "SELECT (username, group_id) FROM membership WHERE username = '" + newMember
					+ "' and group_id = " + groupID;
			PreparedStatement searchStatement = connection.prepareStatement(search);
			ResultSet searchResult;
			searchResult = searchStatement.executeQuery();
			if (searchResult.next()) {
				return true;
			}
			String insertTable = "INSERT INTO membership " + "(username, group_id) VALUES" + "(?,?)";
			PreparedStatement preparedS = connection.prepareStatement(insertTable);
			preparedS.setString(1, newMember);
			preparedS.setInt(2, groupID);
			preparedS.executeUpdate();
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
		}
		return false;
	}

	/**
	 * Creates student groups for an assignment.
	 *
	 * Finds all students who are defined in the Users table and puts each of
	 * them into a group for the assignment. Suppose there are n. Each group
	 * will be of the maximum size allowed for the assignment (call that k),
	 * except for possibly one group of smaller size if n is not divisible by k.
	 * Note that k may be as low as 1.
	 *
	 * The choice of which students to put together is based on their grades on
	 * another assignment, as recorded in table Results. Starting from the
	 * highest grade on that other assignment, the top k students go into one
	 * group, then the next k students go into the next, and so on. The last n %
	 * k students form a smaller group.
	 *
	 * In the extreme case that there are no students, does nothing and returns
	 * true.
	 *
	 * Students with no grade recorded for the other assignment come at the
	 * bottom of the list, after students who received zero. When there is a tie
	 * for grade (or non-grade) on the other assignment, takes students in order
	 * by username, using alphabetical order from A to Z.
	 *
	 * When a group is created, its group ID is generated automatically because
	 * the group_id attribute of table AssignmentGroup is of type SERIAL. The
	 * value of attribute repo is repoPrefix + "/group_" + group_id
	 *
	 * Does nothing and returns false if there is no assignment with ID
	 * assignmentToGroup or no assignment with ID otherAssignment, or if any
	 * group has already been defined for this assignment.
	 *
	 * @param assignmentToGroup
	 *            the assignment ID of the assignment for which groups are to be
	 *            created
	 * @param otherAssignment
	 *            the assignment ID of the other assignment on which the
	 *            grouping is to be based
	 * @param repoPrefix
	 *            the prefix of the URL for the group's repository
	 * @return true if successful and false otherwise
	 */
	public boolean createGroups(int assignmentToGroup, int otherAssignment, String repoPrefix) {
		// Replace this return statement with an implementation of this method!
		String queryString = "SET search_path TO markus";
		PreparedStatement pStatement;
		try {
			pStatement = connection.prepareStatement(queryString);
			pStatement.execute();
			// Check if group is at max
			ResultSet all_students;
			queryString = "select username from MarkusUser where type='student'";
			PreparedStatement ps = connection.prepareStatement(queryString);
			all_students = ps.executeQuery();
			System.out.println("got all usernames");

			ArrayList usernames_list = new ArrayList();

			while (all_students.next()) {
				String name = all_students.getString("username");
				usernames_list.add(name);
			}

			System.out.println("all student usernames" + usernames_list);
			if (usernames_list.size() == 0) {
				return false;
			}
			// get group size

			ResultSet max_group_id;
			queryString = "SELECT max(group_id) from assignmentgroup";
			PreparedStatement ps4 = connection.prepareStatement(queryString);
			max_group_id = ps4.executeQuery();
			int max_id = 1;
			if (max_group_id.next()) {
				max_id = max_group_id.getInt("max") + 1;
				ResultSet set_val;
				queryString = "select setval('assignmentgroup_group_id_seq', 1003)";
				PreparedStatement ps5 = connection.prepareStatement(queryString);
				ps5.executeQuery();
			}

			PreparedStatement ps1;
			int group_max;
			String qs = "SELECT group_max FROM assignment WHERE assignment_id=" + Integer.toString(assignmentToGroup);
			ps1 = connection.prepareStatement(qs);
			ResultSet rs1 = ps1.executeQuery();
			if (rs1.next()) {
				group_max = rs1.getInt("group_max");
				System.out.println("GRUOP MAX IS " + group_max);
			} else {
				return false;
			}

			// get marks for groups from other assignment
			PreparedStatement ps2;
			String qs2 = "SELECT DISTINCT group_id, mark FROM Result WHERE group_id in (SELECT group_id from AssignmentGroup WHERE assignment_id="
					+ otherAssignment + ") ORDER BY mark DESC";
			ps2 = connection.prepareStatement(qs2);
			ResultSet rs2 = ps2.executeQuery();

			ArrayList groups_list = new ArrayList();
			// loop highest mark to lowest
			PreparedStatement ps3;
			ResultSet rs3;
			String qs3;

			ArrayList seen_usernames = new ArrayList();
			ArrayList curr_array = new ArrayList();

			while (rs2.next()) {
				int group_id = rs2.getInt("group_id");
				System.out.println("dis is high group id: " + group_id);
				qs3 = "SELECT username FROM Membership where group_id=" + group_id;
				ps3 = connection.prepareStatement(qs3);
				rs3 = ps3.executeQuery();

				// get members in current group
				while (rs3.next()) {
					String username = rs3.getString("username");
					if (!seen_usernames.contains(username) && usernames_list.contains(username)) {
						// add to an array
						if (curr_array.size() == group_max) {
							groups_list.add(curr_array);
							curr_array = new ArrayList();
							// clear curr_array
							curr_array.add(username);
						} else {
							curr_array.add(username);
						}
						// add to seen
						seen_usernames.add(username);
					}
				}
			}
			groups_list.add(curr_array);
			ArrayList added_members = new ArrayList();
			// loop through array list and make an assignment group for them,
			// and add them to added members
			for (ArrayList group : (ArrayList<ArrayList>) groups_list) {
				Statement statement = connection.createStatement();
				String insertTable = "INSERT INTO assignmentgroup " + "(group_id, assignment_id, repo) VALUES"
						+ "(?, ?,'repo')";
				PreparedStatement preparedS = connection.prepareStatement(insertTable);
				preparedS.setInt(1, max_id);
				preparedS.setInt(2, assignmentToGroup);
				preparedS.executeUpdate();

				// Update repoprefix, repoPrefix + "/group_" + group_id
				String repoString = repoPrefix + "/group_" + max_id;
				Statement statement2 = connection.createStatement();
				String updateTable = "UPDATE assignmentgroup SET repo = ? WHERE group_id=?";
				PreparedStatement preparedS1 = connection.prepareStatement(updateTable);
				preparedS1.setString(1, repoString);
				preparedS1.setInt(2, max_id);
				preparedS1.executeUpdate();

				for (String curr_username : (ArrayList<String>) group) {
					added_members.add(curr_username);
					// make membership
					Statement statement3 = connection.createStatement();
					String insertMember = "INSERT INTO membership Values (?,?)";
					PreparedStatement preparedS2 = connection.prepareStatement(insertMember);
					preparedS2.setString(1, curr_username);
					preparedS2.setInt(2, max_id);
					preparedS2.executeUpdate();
				}
				max_id++;
			}

			// loop thru usernames, and if they are not in added_members then
			// make groups for them and add membership
			int group_size = 0;
			boolean shouldCreateGroup = true;
			boolean firstTime = true;
			for (String name : (ArrayList<String>) usernames_list) {
				if (group_size == group_max) {
					group_size = 0;
					shouldCreateGroup = true;
				} else {
					shouldCreateGroup = false;
				}
				int created_group_id = 0;
				if ((firstTime && !added_members.contains(name))
						|| (!added_members.contains(name) && shouldCreateGroup)) {
					firstTime = false;
					Statement statement = connection.createStatement();
					String insertTable = "INSERT INTO assignmentgroup " + "(group_id, assignment_id, repo) VALUES"
							+ "(?, ?,'repo')";
					PreparedStatement preparedS = connection.prepareStatement(insertTable);
					preparedS.setInt(1, max_id);
					preparedS.setInt(2, assignmentToGroup);
					preparedS.executeUpdate();

					String repoString = repoPrefix + "/group_" + max_id;
					Statement statement2 = connection.createStatement();
					String updateTable = "UPDATE assignmentgroup SET repo = ? WHERE group_id=?";
					PreparedStatement preparedS1 = connection.prepareStatement(updateTable);
					preparedS1.setString(1, repoString);
					preparedS1.setInt(2, max_id);
					preparedS1.executeUpdate();
				}
				// create membership
				if (!added_members.contains(name)) {
					added_members.add(name);
					// make membership
					Statement statement3 = connection.createStatement();
					String insertMember = "INSERT INTO membership Values (?,?)";
					PreparedStatement preparedS2 = connection.prepareStatement(insertMember);
					preparedS2.setString(1, name);
					preparedS2.setInt(2, max_id);
					preparedS2.executeUpdate();
					group_size++;
					max_id++;
				}
			}
			return true;
		} catch (SQLException e) {
			e.printStackTrace();
			return true;
		}
	}

	public static void main(String[] args) throws SQLException {
		Assignment2 a2 = new Assignment2();
		String url = "jdbc:postgresql://localhost:5432/csc343h-chenji13";
		String username = "chenji13";
		String password = "";
		a2.connectDB(url, username, password);
		a2.assignGrader(2000, "i1");
		// a2.recordMember(1002, 2007, "s5");
		// a2.recordMember(1002, 2007, "t1");
		// a2.recordMember(1002, 2007, "s1");
		a2.disconnectDB();
	}
}
