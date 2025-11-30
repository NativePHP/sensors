# FaceDetection Plugin for NativePHP Mobile

A NativePHP Mobile plugin

## Installation

```bash
composer require nativephp/face-detection
```

## Usage

```php
use Nativephp\FaceDetection\Facades\FaceDetection;

// Execute functionality
$result = FaceDetection::execute(['option1' => 'value']);

// Get status
$status = FaceDetection::getStatus();
```

## Listening for Events

```php
use Livewire\Attributes\On;

#[On('native:Nativephp\FaceDetection\Events\FaceDetectionCompleted')]
public function handleFaceDetectionCompleted($result, $id = null)
{
    // Handle the event
}
```

## License

MIT