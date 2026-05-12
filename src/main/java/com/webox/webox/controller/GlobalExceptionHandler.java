package com.webox.webox.controller;

import com.webox.webox.support.BusinessException;
import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.servlet.ModelAndView;
import org.springframework.web.servlet.NoHandlerFoundException;

@ControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(BusinessException.class)
    @ResponseStatus(HttpStatus.BAD_REQUEST)
    public ModelAndView handleBusiness(BusinessException ex) {
        ModelAndView mv = new ModelAndView("error/business");
        mv.setStatus(HttpStatus.BAD_REQUEST);
        mv.addObject("message", ex.getMessage());
        return mv;
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    @ResponseStatus(HttpStatus.NOT_FOUND)
    public ModelAndView handleNotFound(NoHandlerFoundException ex) {
        ModelAndView mv = new ModelAndView("error/404");
        mv.setStatus(HttpStatus.NOT_FOUND);
        return mv;
    }

    @ExceptionHandler(Exception.class)
    @ResponseStatus(HttpStatus.INTERNAL_SERVER_ERROR)
    public ModelAndView handleOther(Exception ex) {
        ModelAndView mv = new ModelAndView("error/500");
        mv.setStatus(HttpStatus.INTERNAL_SERVER_ERROR);
        mv.addObject("message", ex.getMessage());
        return mv;
    }
}
