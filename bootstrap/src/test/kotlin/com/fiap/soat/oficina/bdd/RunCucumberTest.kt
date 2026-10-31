package com.fiap.soat.oficina.bdd

import io.cucumber.junit.platform.engine.Constants
import org.junit.platform.suite.api.ConfigurationParameter
import org.junit.platform.suite.api.IncludeEngines
import org.junit.platform.suite.api.SelectDirectories
import org.junit.platform.suite.api.Suite

@Suite
@IncludeEngines("cucumber")
@SelectDirectories("docs/bdd")
@ConfigurationParameter(key = Constants.GLUE_PROPERTY_NAME, value = "com.fiap.soat.oficina.bdd")
@ConfigurationParameter(key = Constants.FILTER_TAGS_PROPERTY_NAME, value = "@implementado")
@ConfigurationParameter(key = Constants.PLUGIN_PROPERTY_NAME, value = "pretty")
class RunCucumberTest
