package com.blockchain.annotations

@Target(AnnotationTarget.TYPE,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE,
    AnnotationTarget.VALUE_PARAMETER)
@Retention(AnnotationRetention.SOURCE)
annotation class MoveCandidate(val whereTo: String, val why: String = "")

@Target(AnnotationTarget.TYPE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.PROPERTY,
    AnnotationTarget.FIELD,
    AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class BurnCandidate(val why: String)

@Target(AnnotationTarget.TYPE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class MergeCandidate(val why: String)

@Target(AnnotationTarget.TYPE,
    AnnotationTarget.CLASS,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.FILE)
@Retention(AnnotationRetention.SOURCE)
annotation class SplitCandiate(val why: String)

@Target(AnnotationTarget.CLASS,
    AnnotationTarget.FIELD,
    AnnotationTarget.FUNCTION,
    AnnotationTarget.EXPRESSION,
    AnnotationTarget.PROPERTY)
@Retention(AnnotationRetention.SOURCE)
annotation class CommonCode(val commonWith: String)

@Retention(AnnotationRetention.SOURCE)
annotation class ButWhy(val whyWhat: String)
