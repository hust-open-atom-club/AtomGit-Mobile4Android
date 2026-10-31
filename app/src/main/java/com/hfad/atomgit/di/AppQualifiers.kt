package com.hfad.atomgit.di

import javax.inject.Qualifier

/** 会话密文目录。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class SessionDirectory

/** 与界面生命周期无关的应用级协程作用域。 */
@Qualifier
@Retention(AnnotationRetention.BINARY)
annotation class ApplicationScope
