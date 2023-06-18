package com.urisproject.facesearch.model;

import com.baomidou.mybatisplus.extension.activerecord.Model;
import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.*;
import lombok.*;
import lombok.experimental.Accessors;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

/**
 * @Author Anthony HE, anthony.zj.he@outlook.com
 * @Date 3/6/2023
 * @Description:
 */
@Data
@EqualsAndHashCode(callSuper = true)
@Accessors(chain = true)
public class User extends Model {

    private static final long serialVersionUID = 1L;

    private String userCode;

    private String userName;

    private String userSex;

    private String userImage;


}
