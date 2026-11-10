from django.db import models
from django.utils import timezone


class Post(models.Model):
    author = models.ForeignKey(
        'auth.User',
        on_delete=models.CASCADE,
    )
    title = models.CharField(max_length=200)
    text = models.TextField()
    created_date = models.DateTimeField(
        default=timezone.now,
    )
    published_date = models.DateTimeField(
        blank=True,
        null=True,
    )
    image = models.ImageField(
        upload_to='images/%Y/%m/%d/',
        default='images/default_error.png',
    )

    acknowledged = models.BooleanField(
        default=False,
    )
    acknowledged_by = models.ForeignKey(
        'auth.User',
        on_delete=models.SET_NULL,
        blank=True,
        null=True,
        related_name='acknowledged_posts',
    )
    acknowledged_at = models.DateTimeField(
        blank=True,
        null=True,
    )

    def publish(self):
        self.published_date = timezone.now()
        self.save()

    def acknowledge(self, user):
        if self.acknowledged:
            return False

        self.acknowledged = True
        self.acknowledged_by = user
        self.acknowledged_at = timezone.now()
        self.save(
            update_fields=[
                'acknowledged',
                'acknowledged_by',
                'acknowledged_at',
            ]
        )

        return True

    def __str__(self):
        return self.title