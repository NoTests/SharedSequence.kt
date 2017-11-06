package org.notests.sharedsequence.annotations

/**
 * Created by markotron on 11/6/17.
 */
@Target(AnnotationTarget.CLASS)
@Retention(AnnotationRetention.SOURCE)
annotation class SharedSequence(val value: String)