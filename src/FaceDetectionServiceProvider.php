<?php

namespace Nativephp\FaceDetection;

use Illuminate\Support\ServiceProvider;

class FaceDetectionServiceProvider extends ServiceProvider
{
    public function register(): void
    {
        $this->app->singleton(FaceDetection::class, function () {
            return new FaceDetection();
        });
    }

    public function boot(): void
    {
        //
    }
}