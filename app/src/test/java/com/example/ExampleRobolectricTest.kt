package com.example

import android.content.Context
import androidx.test.core.app.ActivityScenario
import androidx.test.core.app.ApplicationProvider
import com.example.data.DatabaseProvider
import com.example.data.entity.*
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNotNull
import org.junit.Test
import org.junit.runner.RunWith
import org.robolectric.RobolectricTestRunner
import org.robolectric.annotation.Config

@RunWith(RobolectricTestRunner::class)
@Config(sdk = [36])
class ExampleRobolectricTest {

  @Test
  fun `read string from context`() {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val appName = context.getString(R.string.app_name)
    assertEquals("Personal CRM", appName)
  }

  @Test
  fun `launch MainActivity successfully`() {
    ActivityScenario.launch(MainActivity::class.java).use { scenario ->
      scenario.onActivity { activity ->
        assertNotNull(activity)
      }
    }
  }

  @Test
  fun `test delete and undo delete contact`() = runBlocking {
    val context = ApplicationProvider.getApplicationContext<Context>()
    val repository = DatabaseProvider.getRepository(context)
    
    // 1. Insert contact
    val contact = Contact(
      firstName = "John",
      lastName = "Doe",
      nickname = "Johnny"
    )
    val contactId = repository.insertContact(contact)
    assert(contactId > 0)
    
    // Insert some phone numbers and notes
    repository.insertPhoneNumber(PhoneNumber(contactId = contactId, number = "12345", label = "mobile"))
    repository.insertNote(Note(contactId = contactId, content = "Some notes"))

    // 2. Retrieve ContactWithDetails to verify
    val fullContact = repository.getContactByIdSuspend(contactId)
    assertNotNull(fullContact)
    assertEquals("John", fullContact!!.contact.firstName)
    assertEquals(1, fullContact.phoneNumbers.size)
    assertEquals(1, fullContact.notes.size)

    // 3. Delete Contact
    repository.deleteContact(fullContact.contact)
    val afterDelete = repository.getContactByIdSuspend(contactId)
    assertEquals(null, afterDelete)

    // 4. Perform Undo Delete manually as inside ViewModel
    repository.insertContact(fullContact.contact)
    for (phone in fullContact.phoneNumbers) {
      repository.insertPhoneNumber(phone)
    }
    for (note in fullContact.notes) {
      repository.insertNote(note)
    }

    // 5. Verify restored contact
    val restored = repository.getContactByIdSuspend(contactId)
    assertNotNull(restored)
    assertEquals("John", restored!!.contact.firstName)
    assertEquals(1, restored.phoneNumbers.size)
    assertEquals(1, restored.notes.size)
  }
}

