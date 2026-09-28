package com.example

import android.content.Context
import androidx.test.core.app.ApplicationProvider
import com.example.data.model.Contribution
import com.example.util.UpiHelper
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Assert.assertTrue
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [34])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Mayan Welfare", appName)
  }

  @Test
  fun `verify upi uri generation`() {
    val uri = UpiHelper.buildUpiUri(
      upiId = "mayanwelfare@okhdfcbank",
      name = "MAYAN's Well Fare",
      amount = 500.0,
      note = "Monthly Contribution"
    )
    assertNotNull(uri)
    assertEquals("upi", uri.scheme)
    assertEquals("mayanwelfare@okhdfcbank", uri.getQueryParameter("pa"))
    assertEquals("500.00", uri.getQueryParameter("am"))
  }

  @Test
  fun `verify contribution constants`() {
    assertEquals("PAID", Contribution.STATUS_PAID)
    assertEquals("PENDING", Contribution.STATUS_PENDING)
    assertEquals("CASH_PENDING_VERIFICATION", Contribution.STATUS_CASH_PENDING)
    assertEquals("OVERDUE", Contribution.STATUS_OVERDUE)
  }

  @Test
  fun `verify overall summary data calculation`() {
    val totalExpected = 10 * 2 * 500.0 // 10 members, 2 months, 500 Rs each
    val totalCollected = 6500.0
    val totalPending = totalExpected - totalCollected
    val rate = (totalCollected / totalExpected) * 100

    assertEquals(10000.0, totalExpected, 0.01)
    assertEquals(3500.0, totalPending, 0.01)
    assertEquals(65.0, rate, 0.01)
  }
}
