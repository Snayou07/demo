package com.example.demo.rest;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class FunRestController {
    //expose / that return "hello word"
    @GetMapping("/")
    public String seyHello(){
        return "Hello word";
    }

    //expose a new endpoint for "workout"

    @GetMapping("/workout")
    public String getDailyWorkout()
        {
        return "Run a hard 5k";
    }

    //expose a new endpoint for "fortune"

    @GetMapping("/fortune")
    public String getFortunenumber ()
    {
        int randomNum = new java.util.Random().nextInt(100);
      return String.valueOf(randomNum);
    }
}
