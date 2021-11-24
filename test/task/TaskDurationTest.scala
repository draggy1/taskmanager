package task

import org.scalatest.GivenWhenThen
import org.scalatestplus.play.PlaySpec

class TaskDurationTest extends PlaySpec with GivenWhenThen  {
  "TaskDuration#createFromString" should {
    "be successful" in {
      Given("duration format in proper way")
      val givenDurationAsString = "3 hours 23 minutes"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 3 hours and 23 minutes as TaskDuration object")
      result.hoursValue mustBe 3
      result.minutesValue mustBe 23
    }
  }

  "TaskDuration#createFromString with single hour and single minute " should {
    "be successful" in {
      Given("duration format in proper way")
      val givenDurationAsString = "1 hour 1 minute"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 1 hour and 1 minute as TaskDuration object")
      result.hoursValue mustBe 1
      result.minutesValue mustBe 1
    }
  }

  "TaskDuration#createFromString with totally random and incorrect string" should {
    "be successful and returns duration with zeros" in {
      Given("totally random and incorrect string")
      val givenDurationAsString = "232sdfhdfkjghd 23dbfgbhjdfgb3 3455dfvhkjbgdfhjsfhgshjdb"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 0 hour and 0 minute as TaskDuration object")
      result.hoursValue mustBe 0
      result.minutesValue mustBe 0
    }
  }

  "TaskDuration#createFromString with only hours" should {
    "be successful and returns correct result" in {
      Given("duration with only hours")
      val givenDurationAsString = "4 hours"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 4 hours")
      result.hoursValue mustBe 4
      result.minutesValue mustBe 0
    }
  }

  "TaskDuration#createFromString with only minutes" should {
    "be successful and returns correct result" in {
      Given("duration with only minutes")
      val givenDurationAsString = "56 minutes"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 56 minutes")
      result.hoursValue mustBe 0
      result.minutesValue mustBe 56
    }
  }

  "TaskDuration#createFromString with too big number of minutes" should {
    "be successful and returns correct result" in {
      Given("too big number of minutes")
      val givenDurationAsString = "11111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
        "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
        "111111111111111 minutes"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 0 minutes")
      result.hoursValue mustBe 0
      result.minutesValue mustBe 0
    }
  }


  "TaskDuration#createFromString with too big number of hours" should {
    "be successful and returns correct result" in {
      Given("too big number of hours")
      val givenDurationAsString = "11111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
        "11111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111111" +
        "111111111111111 hours"

      When("createFromString is performed")
      val result = TaskDuration.createFromString(givenDurationAsString)

      Then("Result: 0 hours")
      result.hoursValue mustBe 0
      result.minutesValue mustBe 0
    }
  }
}
