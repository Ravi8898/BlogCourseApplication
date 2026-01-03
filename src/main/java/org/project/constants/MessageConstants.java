package org.project.constants;

public class MessageConstants {
    //    Status messages
    public static final String SUCCESS = "SUCCESS";
    public static final String FAILED = "FAILED";
    public static final String ERROR = "ERROR";

    //    Registration messages
    public static final String USERNAME_PASSWORD_REQUIRED = "Username and password are required.";
    public static final String USERNAME_EXISTS = "Username already exists.";
    public static final String USER_NOT_FOUND = "User not found.";
    public static final String REGISTRATION_SUCCESSFUL = "Registration successful.";

    //    Login messages
    public static final String LOGIN_SUCCESS = "Login successful!";
    public static final String LOGIN_FAILED = "Login failed. Invalid credentials.";
    public static final String BAD_REQUEST = "Bad request.";
    public static final String SOMETHING_WENT_WRONG = "Something went wrong. Please try again later.";

    //    JWT messages
    public static final String INVALID_TOKEN = "Invalid token.";
    public static final String LOGOUT_SUCCESS = "Logout successful.";
    public static final String LOGOUT_FAILED = "Logout failed.";
    public static final String AUTH_HEADER_MISSING = "Authorization header missing";
    public static final String TOKEN_EXPIRED = "Token expired";
    public static final String TOKEN_INVALIDATED = "Token has been invalidated";
    public static final String ACCESS_DENIED = "Access denied";

    //    User Token messages
    public static final String ALL_SESSION_LOGOUT_SUCCESS = "All sessions logout successfully.";
    public static final String ALL_SESSION_LOGOUT_FAILED = "All sessions logout failed.";
    public static final String ALL_SESSION_LOGOUT_FAILED_500 = "All sessions logout failed. Something went wrong.";

    //User messages
    public static final String FETCH_USERS_SUCCESS ="Users fetched successfully.";
    public static final String FETCH_USERS_FAILED = "Failed to fetch users. Something went wrong.";
    public static final String NO_USERS_FOUND = "No users found.";
    public static final String DELETE_USER_SUCCESS = "User account deleted successfully.";
    public static final String DELETE_USER_FAILED = "Failed to delete user.";
    public static final String PROFILE_UPDATE_SUCCESS = "Profile updated successfully";
    public static final String PROFILE_UPDATE_FAILED = "Profile updation failed. Something went wrong.";
    public static final String LOGOUT_ON_PROFILE_UPDATE = "Profile updated. Please login again.";

    //Article message
    public static final String ARTICLE_CREATED_SUCCESS = "Article created successfully";
    public static final String ARTICLE_CREATED_FAILED = "Article creation failed. Something went wrong.";
    public static final String ARTICLE_FILE_SAVE_FAILED = "Failed to save article content file.";
    public static final String ARTICLE_NOT_FOUND = "Article not found.";
    public static final String FETCH_ARTICLE_SUCCESS = "Article fetched successfully.";
}
