package lk.sliit.se3070.group058.disasterlink.shared.web;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.view;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.web.servlet.MockMvc;

/** Tests of {@link HomeController} and of the static files the pages load. */
@WebMvcTest(HomeController.class)
class HomeControllerTest {

  @Autowired private MockMvc mockMvc;

  @Test
  @DisplayName("The home page renders with the title and one link per page root")
  void homePage() throws Exception {
    mockMvc
        .perform(get("/"))
        .andExpect(status().isOk())
        .andExpect(view().name("index"))
        .andExpect(
            content()
                .string(
                    org.hamcrest.Matchers.containsString(
                        "DisasterLink — DMC Sri Lanka (prototype)")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/warning\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/hazardreport\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/shelter\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/analytics\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/organisation\"")))
        .andExpect(content().string(org.hamcrest.Matchers.containsString("href=\"/css/app.css\"")));
  }

  @ParameterizedTest(name = "{0}")
  @ValueSource(
      strings = {
        "/css/app.css",
        "/js/district-map.js",
        "/js/districts.js",
        "/js/nav-notice.js",
        "/js/report-charts.js",
        "/vendor/leaflet-1.9.4/leaflet.js",
        "/vendor/leaflet-1.9.4/leaflet.css",
        "/vendor/leaflet-1.9.4/images/layers.png",
        "/vendor/leaflet-1.9.4/LICENSE",
        "/vendor/chartjs-4.4.4/chart.umd.js",
        "/vendor/chartjs-4.4.4/LICENSE.md",
        "/fonts/Manrope-VariableFont_wght.ttf",
        "/fonts/NotoSansSinhala-Regular.ttf",
        "/fonts/NotoSansTamil-Regular.ttf"
      })
  @DisplayName("The static files and vendor libraries are served")
  void staticFilesAreServed(String path) throws Exception {
    mockMvc.perform(get(path)).andExpect(status().isOk());
  }
}
