<?php

namespace Nativephp\FaceDetection\Facades;

use Illuminate\Support\Facades\Facade;

/**
 * @method static mixed execute(array $options = [])
 * @method static object|null getStatus()
 *
 * @see \Nativephp\FaceDetection\FaceDetection
 */
class FaceDetection extends Facade
{
    protected static function getFacadeAccessor(): string
    {
        return \Nativephp\FaceDetection\FaceDetection::class;
    }
}