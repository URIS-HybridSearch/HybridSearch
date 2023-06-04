package com.urisproject.facesearch.service;


import com.baomidou.mybatisplus.extension.service.IService;
import com.urisproject.facesearch.model.User;

import java.io.IOException;

/**
 * Service interface for managing users.
 */
public interface UserService extends IService<User> {

    /**
     * Adds a new user to the system.
     *
     * @param name        the name of the user
     * @param gender      the gender of the user
     * @param imageBase64 the base64-encoded image data for the user's face
     * @param feature     the feature vector for the user's face
     * @return a message indicating the success or failure of the operation
     * @throws IOException if there is an error reading or writing data
     */
    String addUser(String name, String gender, String imageBase64, String feature) throws IOException;

}