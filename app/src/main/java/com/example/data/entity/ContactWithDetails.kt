package com.example.data.entity

import androidx.room.Embedded
import androidx.room.Relation

data class ContactWithDetails(
    @Embedded val contact: Contact,
    
    @Relation(parentColumn = "id", entityColumn = "contactId")
    val phoneNumbers: List<PhoneNumber> = emptyList(),
    
    @Relation(parentColumn = "id", entityColumn = "contactId")
    val emails: List<Email> = emptyList(),
    
    @Relation(parentColumn = "id", entityColumn = "contactId")
    val customActions: List<CustomAction> = emptyList(),
    
    @Relation(parentColumn = "id", entityColumn = "contactId")
    val notes: List<Note> = emptyList(),
    
    @Relation(parentColumn = "id", entityColumn = "contactId")
    val addresses: List<Address> = emptyList()
)
