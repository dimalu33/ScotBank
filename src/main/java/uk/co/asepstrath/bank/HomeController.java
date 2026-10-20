package uk.co.asepstrath.bank;

import io.jooby.ModelAndView;
import io.jooby.annotation.GET;
import io.jooby.annotation.Path;

import java.util.HashMap;
import java.util.Map;

@Path("/")
public class HomeController {

    @GET
    public ModelAndView<Map<String, Object>> home() {
        Map<String, Object> model = new HashMap<>();
        return new ModelAndView<>("home.hbs", model);
    }
}
