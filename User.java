/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package mailProject;

import java.io.Serializable;
/**
 *
 * @author ryan
 */
public class User implements Serializable{
    private String firstName = "";
    private String lastName = "";
    private String addr;
    private String password;

    public User() {
    }

    public User(String firstName, String lastName, String password, String addr) {
        this.firstName = firstName;
        this.lastName = lastName;
        this.addr = addr;
        this.password = password;
    }

    public String getFirstName() {
        return firstName;
    }

    public void setFirstName(String firstName) {
        this.firstName = firstName;
    }

    public String getLastName() {
        return lastName;
    }

    public void setLastName(String lastName) {
        this.lastName = lastName;
    }

    public String getPassword() {
        return password;
    }

    public void setPassword(String password) {
        this.password = password;
    }

    public String getAddr() {
        return addr;
    }

    public void setAddr(String addr) {
        this.addr = addr;
    }
    
    
}