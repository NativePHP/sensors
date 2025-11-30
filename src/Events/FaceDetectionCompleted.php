<?php

namespace Nativephp\FaceDetection\Events;

use Illuminate\Foundation\Events\Dispatchable;
use Illuminate\Queue\SerializesModels;

class FaceDetectionCompleted
{
    use Dispatchable, SerializesModels;

    public function __construct(
        public bool $success,
        public array $faces = [],
        public int $count = 0,
        public array $imageMetadata = [],
        public ?string $error = null,
        public ?string $id = null
    ) {}
}
