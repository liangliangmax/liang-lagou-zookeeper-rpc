package com.lagou.rpc.consumer.controller;


import com.lagou.rpc.api.IUserService;
import com.lagou.rpc.consumer.annotation.RpcReference;
import com.lagou.rpc.pojo.User;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/user")
public class UserController {


    @RpcReference
    private IUserService userService;


    @GetMapping("/getUserById")
    public User getUserById(@RequestParam("id") Integer id){

        User user = userService.getById(id);

        return user;
    }

}
