package org.notests.sharedsequence.api

/**
 * Created by markotron on 11/6/17.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SharedSequence(val value: String)