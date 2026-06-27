package com.example.io_motion.core.pose;

import android.content.Context;
import dagger.internal.DaggerGenerated;
import dagger.internal.Factory;
import dagger.internal.QualifierMetadata;
import dagger.internal.ScopeMetadata;
import javax.annotation.processing.Generated;
import javax.inject.Provider;

@ScopeMetadata
@QualifierMetadata("dagger.hilt.android.qualifiers.ApplicationContext")
@DaggerGenerated
@Generated(
    value = "dagger.internal.codegen.ComponentProcessor",
    comments = "https://dagger.dev"
)
@SuppressWarnings({
    "unchecked",
    "rawtypes",
    "KotlinInternal",
    "KotlinInternalInJava",
    "cast"
})
public final class PoseFrameSource_Factory implements Factory<PoseFrameSource> {
  private final Provider<Context> contextProvider;

  public PoseFrameSource_Factory(Provider<Context> contextProvider) {
    this.contextProvider = contextProvider;
  }

  @Override
  public PoseFrameSource get() {
    return newInstance(contextProvider.get());
  }

  public static PoseFrameSource_Factory create(Provider<Context> contextProvider) {
    return new PoseFrameSource_Factory(contextProvider);
  }

  public static PoseFrameSource newInstance(Context context) {
    return new PoseFrameSource(context);
  }
}
