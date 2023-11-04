package cn.cnic.instdb.model.center;

import lombok.Data;

import javax.validation.constraints.NotNull;

/**
 *  Institutional entities corresponding to the main center
 */

@Data
public class Org {

    @NotNull(message = "Chinese name cannot be empty")
    private String zh_Name;   //Chinese name

    @NotNull(message = "Institution address cannot be empty")
    private String address;   //Institution address

    @NotNull(message = "Institutional nature cannot be empty")
    private String nature;    //Institutional nature

    private String en_Name;    // English name
    private String number;     //Institution code
    private String introduction;  //Institutional Introduction
    private String telephone;    //Contact number
    private String email;         //Contact email

    private String account;       //Permission account
    private String password;      //password
}
